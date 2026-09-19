/**
 * Trigger App — Cloud Functions
 *
 * 1. OTP email verification (signup + password reset) with 6-digit codes:
 *      - sendOtp               : emails a 6-digit code (60s cooldown, 10min expiry)
 *      - verifyOtp             : checks a code without consuming it (counts attempts)
 *      - signUpWithOtp         : verifies code, creates the account + unique username claim,
 *                                returns a custom token so the app is signed in instantly
 *      - resetPasswordWithOtp  : verifies code, sets the new password, returns a custom token
 *      - changeUsername        : renames the unique handle for the signed-in user
 * 2. onNewChatMessage: FCM data push when a row is created under `Chats/{id}`.
 *
 * Email delivery uses SMTP configured via `functions/.env` (see .env.example):
 *   SMTP_HOST / SMTP_PORT / SMTP_USER / SMTP_PASS / SMTP_FROM / SMTP_FROM_NAME
 *   or a single SMTP_URL=smtps://user:pass@host:port
 *
 * Deploy: npm install && firebase deploy --only functions
 * Requires Blaze (already enabled on this project).
 */

const {onValueCreated} = require("firebase-functions/v2/database");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {getDatabase} = require("firebase-admin/database");
const {getMessaging} = require("firebase-admin/messaging");
const crypto = require("crypto");
const nodemailer = require("nodemailer");

initializeApp();

const NODE_USERS = "Users";
const NODE_TOKENS = "Tokens";
const NODE_USERNAMES = "Usernames";
const NODE_OTPS = "Otps";
const CHILD_USERNAME = "username";
const CHILD_TOKEN = "token";

const FCM_USER = "user";
const FCM_ICON = "icon";
const FCM_BODY = "body";
const FCM_TITLE = "title";
const FCM_SENT = "sent";

const OTP_TTL_MS = 10 * 60 * 1000; // code valid for 10 minutes
const OTP_RESEND_COOLDOWN_MS = 60 * 1000; // 60 seconds between sends
const OTP_MAX_ATTEMPTS = 5;
const CODE_LENGTH = 6;

const USERNAME_RE = /^[A-Za-z0-9_]{6,25}$/;

/** Stable error tokens understood by the Android client (mapped to localized strings). */
const ERR = {
  INVALID_EMAIL: "EMAIL_INVALID",
  EMAIL_IN_USE: "EMAIL_IN_USE",
  EMAIL_NOT_FOUND: "EMAIL_NOT_FOUND",
  OTP_INVALID: "OTP_INVALID",
  OTP_EXPIRED: "OTP_EXPIRED",
  OTP_TOO_MANY_ATTEMPTS: "OTP_TOO_MANY_ATTEMPTS",
  OTP_COOLDOWN: "OTP_COOLDOWN",
  USERNAME_TAKEN: "USERNAME_TAKEN",
  USERNAME_INVALID: "USERNAME_INVALID",
  WEAK_PASSWORD: "WEAK_PASSWORD",
  OTP_MISMATCH_PURPOSE: "OTP_INVALID",
  SERVICE_NOT_CONFIGURED: "EMAIL_SERVICE_NOT_CONFIGURED",
  SEND_FAILED: "OTP_SEND_FAILED",
  NOT_SIGNED_IN: "NOT_SIGNED_IN",
  SAME_USERNAME: "USERNAME_UNCHANGED",
};

/** Realtime Database keys cannot contain . # $ / [ ] — encode an email to a safe key. */
function emailKey(email) {
  return email.toLowerCase().replace(/[.#$/[\]]/g, "_");
}

function otpRef(purpose, email) {
  const db = getDatabase();
  return db.ref(`${NODE_OTPS}/${purpose}_${emailKey(email)}`);
}

/** SHA-256 hash of the code (with optional pepper) — raw codes are never stored. */
function hashCode(code) {
  const pepper = process.env.OTP_PEPPER || "";
  return crypto.createHash("sha256").update(`${code}:${pepper}`).digest("hex");
}

function generateCode() {
  return crypto.randomInt(0, 1000000).toString().padStart(CODE_LENGTH, "0");
}

/**
 * Verifies a submitted code. Throws HttpsError on any failure.
 * @param {string} purpose "signup" | "reset"
 * @param {boolean} consume When true the stored code is deleted after success.
 */
async function verifyOtpInternal(purpose, email, code, consume) {
  if (!code || !/^\d{6}$/.test(code)) {
    throw new HttpsError("invalid-argument", ERR.OTP_INVALID);
  }
  const ref = otpRef(purpose, email);
  const snap = await ref.get();
  if (!snap.exists()) {
    throw new HttpsError("failed-precondition", ERR.OTP_EXPIRED);
  }
  const rec = snap.val() || {};
  if (Number(rec.expiresAt || 0) < Date.now()) {
    await ref.remove();
    throw new HttpsError("failed-precondition", ERR.OTP_EXPIRED);
  }
  const attempts = Number(rec.attempts || 0);
  if (attempts >= OTP_MAX_ATTEMPTS) {
    await ref.remove();
    throw new HttpsError("resource-exhausted", ERR.OTP_TOO_MANY_ATTEMPTS);
  }
  if (hashCode(String(code)) !== rec.codeHash) {
    await ref.child("attempts").set(attempts + 1);
    if (attempts + 1 >= OTP_MAX_ATTEMPTS) {
      await ref.remove();
      throw new HttpsError("resource-exhausted", ERR.OTP_TOO_MANY_ATTEMPTS);
    }
    throw new HttpsError("invalid-argument", ERR.OTP_INVALID);
  }
  if (consume) {
    await ref.remove();
  }
  return true;
}

/** Lazily created nodemailer transport from environment configuration. */
let cachedTransport = null;
function getTransport() {
  const {
    SMTP_URL,
    SMTP_HOST,
    SMTP_PORT,
    SMTP_USER,
    SMTP_PASS,
  } = process.env;
  if (SMTP_URL) {
    return nodemailer.createTransport(SMTP_URL);
  }
  if (SMTP_HOST && SMTP_USER && SMTP_PASS) {
    const port = Number(SMTP_PORT || 465);
    return nodemailer.createTransport({
      host: SMTP_HOST,
      port,
      secure: port === 465,
      auth: {user: SMTP_USER, pass: SMTP_PASS},
    });
  }
  return null;
}

function otpEmailHtml(code, purpose) {
  const title = purpose === "signup"
    ? "Verify your email" : "Password reset code";
  const note = purpose === "signup"
    ? "Use this code to finish creating your Trigger App account."
    : "Use this code to reset your Trigger App password.";
  return `
  <div style="background:#0b0b0b;padding:32px;font-family:Arial,Helvetica,sans-serif;color:#ffffff;">
    <div style="max-width:480px;margin:0 auto;background:#141414;border-radius:16px;padding:32px;text-align:center;">
      <h2 style="margin:0 0 8px;color:#ffffff;">${title}</h2>
      <p style="margin:0 0 24px;color:#AFACAC;font-size:14px;">${note}</p>
      <div style="letter-spacing:10px;font-size:36px;font-weight:bold;color:#63FFA3;margin-bottom:24px;">${code}</div>
      <p style="margin:0 0 6px;color:#AFACAC;font-size:13px;">This code expires in 10 minutes.</p>
      <p style="margin:0;color:#797B7E;font-size:12px;">Never share this code with anyone.</p>
    </div>
  </div>`;
}

async function sendOtpEmail(email, code, purpose) {
  const transport = getTransport();
  if (!transport) {
    throw new HttpsError("failed-precondition", ERR.SERVICE_NOT_CONFIGURED);
  }
  const fromName = process.env.SMTP_FROM_NAME || "Trigger App";
  const from = process.env.SMTP_FROM || process.env.SMTP_USER;
  const subject = purpose === "signup"
    ? `${code} is your Trigger App verification code`
    : `${code} is your Trigger App password reset code`;
  try {
    await transport.sendMail({
      from: `${fromName} <${from}>`,
      to: email,
      subject,
      text: `Your Trigger App code is ${code}. It expires in 10 minutes. Never share this code.`,
      html: otpEmailHtml(code, purpose),
    });
  } catch (e) {
    console.error("SMTP send failed", e);
    throw new HttpsError("internal", ERR.SEND_FAILED);
  }
}

async function assertEmailUsable(email, purpose) {
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpsError("invalid-argument", ERR.INVALID_EMAIL);
  }
  if (purpose === "signup") {
    try {
      await getAuth().getUserByEmail(email);
      throw new HttpsError("already-exists", ERR.EMAIL_IN_USE);
    } catch (e) {
      if (e instanceof HttpsError) throw e;
      if (e.code !== "auth/user-not-found") throw e;
    }
  } else {
    try {
      await getAuth().getUserByEmail(email);
    } catch (e) {
      if (e.code === "auth/user-not-found") {
        throw new HttpsError("not-found", ERR.EMAIL_NOT_FOUND);
      }
      throw e;
    }
  }
}

/** Stores a fresh code for (email, purpose) enforcing the resend cooldown. */
async function issueOtp(purpose, email) {
  const ref = otpRef(purpose, email);
  const snap = await ref.get();
  if (snap.exists()) {
    const sentAt = Number((snap.val() || {}).sentAt || 0);
    const waitLeft = OTP_RESEND_COOLDOWN_MS - (Date.now() - sentAt);
    if (waitLeft > 0) {
      throw new HttpsError("failed-precondition", ERR.OTP_COOLDOWN);
    }
  }
  const code = generateCode();
  await ref.set({
    codeHash: hashCode(code),
    expiresAt: Date.now() + OTP_TTL_MS,
    sentAt: Date.now(),
    attempts: 0,
    email: email.toLowerCase(),
    purpose,
  });
  await sendOtpEmail(email, code, purpose);
  return {ok: true, cooldownSeconds: OTP_RESEND_COOLDOWN_MS / 1000};
}

function validUsernameOrThrow(username) {
  if (typeof username !== "string" || !USERNAME_RE.test(username)) {
    throw new HttpsError("invalid-argument", ERR.USERNAME_INVALID);
  }
}

function validPasswordOrThrow(password) {
  if (typeof password !== "string" || password.length < 6 || password.length > 72) {
    throw new HttpsError("invalid-argument", ERR.WEAK_PASSWORD);
  }
}

// ---------------------------------------------------------------------------
// Callable: sendOtp — email a 6-digit code (signup or password reset)
// ---------------------------------------------------------------------------
exports.sendOtp = onCall(
    {
      region: "us-central1",
      timeoutSeconds: 60,
      memory: "256MB",
    },
    async (request) => {
      const {email, purpose: rawPurpose} = request.data || {};
      const purpose = rawPurpose === "reset" ? "reset" : "signup";
      const cleanEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
      await assertEmailUsable(cleanEmail, purpose);
      return issueOtp(purpose, cleanEmail);
    },
);

// ---------------------------------------------------------------------------
// Callable: verifyOtp — check a code without consuming it
// ---------------------------------------------------------------------------
exports.verifyOtp = onCall(
    {
      region: "us-central1",
      timeoutSeconds: 30,
      memory: "256MB",
    },
    async (request) => {
      const {email, purpose: rawPurpose, code} = request.data || {};
      const purpose = rawPurpose === "reset" ? "reset" : "signup";
      const cleanEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
      await verifyOtpInternal(purpose, cleanEmail, code, false);
      return {ok: true};
    },
);

// ---------------------------------------------------------------------------
// Callable: signUpWithOtp — verify code, create account, claim username,
// return a custom token so the app signs in instantly.
// ---------------------------------------------------------------------------
exports.signUpWithOtp = onCall(
    {
      region: "us-central1",
      timeoutSeconds: 60,
      memory: "256MB",
    },
    async (request) => {
      const {email, code, username, password} = request.data || {};
      const cleanEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
      validUsernameOrThrow(username);
      validPasswordOrThrow(password);

      await assertEmailUsable(cleanEmail, "signup");
      await verifyOtpInternal("signup", cleanEmail, code, true);

      const handleLower = String(username).toLowerCase();
      const db = getDatabase();
      const handleSnap = await db.ref(`${NODE_USERNAMES}/${handleLower}`).get();
      if (handleSnap.exists()) {
        throw new HttpsError("already-exists", ERR.USERNAME_TAKEN);
      }

      let uid;
      try {
        const user = await getAuth().createUser({
          email: cleanEmail,
          password,
          displayName: String(username),
        });
        uid = user.uid;
      } catch (e) {
        if (e.code === "auth/email-already-exists") {
          throw new HttpsError("already-exists", ERR.EMAIL_IN_USE);
        }
        throw e;
      }

      const profile = {
        id: uid,
        username: String(username),
        emailId: cleanEmail,
        timestamp: Date.now().toString(),
        imageUrl: "default",
        bio: "Hey there!",
        status: "offline",
        search: handleLower,
        lastSeen: 0,
      };

      try {
        await db.ref("/").update({
          [`${NODE_USERS}/${uid}`]: profile,
          [`${NODE_USERNAMES}/${handleLower}`]: uid,
        });
      } catch (e) {
        console.error("Profile write failed; deleting orphan auth user", e);
        try {
          await getAuth().deleteUser(uid);
        } catch (cleanupErr) {
          console.error("Cleanup failed", cleanupErr);
        }
        throw new HttpsError("internal", ERR.SEND_FAILED);
      }

      const customToken = await getAuth().createCustomToken(uid);
      return {ok: true, customToken};
    },
);

// ---------------------------------------------------------------------------
// Callable: resetPasswordWithOtp — verify code, set new password, return
// a custom token so the app can sign the user in immediately.
// ---------------------------------------------------------------------------
exports.resetPasswordWithOtp = onCall(
    {
      region: "us-central1",
      timeoutSeconds: 60,
      memory: "256MB",
    },
    async (request) => {
      const {email, code, newPassword} = request.data || {};
      const cleanEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
      validPasswordOrThrow(newPassword);
      await verifyOtpInternal("reset", cleanEmail, code, true);
      try {
        const user = await getAuth().getUserByEmail(cleanEmail);
        await getAuth().updateUser(user.uid, {password: newPassword});
        const customToken = await getAuth().createCustomToken(user.uid);
        return {ok: true, customToken};
      } catch (e) {
        if (e.code === "auth/user-not-found") {
          throw new HttpsError("not-found", ERR.EMAIL_NOT_FOUND);
        }
        throw e;
      }
    },
);

// ---------------------------------------------------------------------------
// Callable: changeUsername — rename the unique handle of the signed-in user.
// ---------------------------------------------------------------------------
exports.changeUsername = onCall(
    {
      region: "us-central1",
      timeoutSeconds: 30,
      memory: "256MB",
    },
    async (request) => {
      const uid = request.auth && request.auth.uid;
      if (!uid) throw new HttpsError("unauthenticated", ERR.NOT_SIGNED_IN);
      const username = request.data && request.data.username;
      validUsernameOrThrow(username);

      const newLower = String(username).toLowerCase();
      const db = getDatabase();
      const claimed = await db.ref(`${NODE_USERNAMES}/${newLower}`).get();
      if (claimed.exists() && claimed.val() !== uid) {
        throw new HttpsError("already-exists", ERR.USERNAME_TAKEN);
      }

      const profileSnap = await db.ref(`${NODE_USERS}/${uid}`).get();
      if (!profileSnap.exists()) {
        throw new HttpsError("failed-precondition", ERR.NOT_SIGNED_IN);
      }
      const profile = profileSnap.val() || {};
      const oldLower = String(profile.search || "").toLowerCase();

      const updates = {
        [`${NODE_USERS}/${uid}/username`]: String(username),
        [`${NODE_USERS}/${uid}/search`]: newLower,
        [`${NODE_USERNAMES}/${newLower}`]: uid,
      };
      if (oldLower && oldLower !== newLower) {
        const oldOwner = await db.ref(`${NODE_USERNAMES}/${oldLower}`).get();
        if (oldOwner.exists() && oldOwner.val() === uid) {
          updates[`${NODE_USERNAMES}/${oldLower}`] = null;
        }
      }
      await db.ref("/").update(updates);
      return {ok: true};
    },
);

// ---------------------------------------------------------------------------
// Trigger: FCM data push on new chat message (unchanged behavior)
// ---------------------------------------------------------------------------
exports.onNewChatMessage = onValueCreated(
    {
      ref: "/Chats/{messageId}",
      region: "us-central1",
    },
    async (event) => {
      const val = event.data.val();
      if (!val || typeof val !== "object") return;

      const receiverId = val.receiverId;
      const senderId = val.senderId;
      const messageText = typeof val.message === "string" ? val.message : "";

      if (!receiverId || !senderId) return;

      const db = getDatabase();

      const tokenSnap = await db.ref(`${NODE_TOKENS}/${receiverId}/${CHILD_TOKEN}`).get();
      const token = tokenSnap.val();
      if (!token || typeof token !== "string") return;

      let senderName = "User";
      const nameSnap = await db.ref(`${NODE_USERS}/${senderId}/${CHILD_USERNAME}`).get();
      if (nameSnap.exists && typeof nameSnap.val() === "string") {
        senderName = nameSnap.val();
      }

      const body = `${senderName}: ${messageText}`;

      try {
        await getMessaging().send({
          token,
          android: {
            priority: "high",
          },
          data: {
            [FCM_USER]: String(senderId),
            [FCM_ICON]: "ic_notification",
            [FCM_BODY]: body,
            [FCM_TITLE]: "New Message",
            [FCM_SENT]: String(receiverId),
          },
        });
      } catch (e) {
        console.error("FCM send failed", e);
      }
    },
);
