# Server-Side Verification — Deploy Guide (Cloud Function + Face++)

This release upgrades the verified badge from a **client-claimed** result to a
**tamper-proof, server-verified** result. The client can no longer write the
verification node — only the Cloud Function can.

## How it works

```
VerificationScreen (app)
  ├─ ML Kit liveness on-device (center face → blink → smile)   ← free, rejects photos
  ├─ TFLite gender pre-check on-device                         ← free fail-fast gate
  └─ uploads the face crop (base64 JPEG ≤ 640px)               ← one HTTPS call
        ↓
Cloud Function `verifyFace` (us-central1)
  ├─ auth required (Firebase ID token from the app)
  ├─ 24 h verdict cache (repeat taps cost 0 Face++ calls)
  ├─ Face++ POST /facepp/v3/detect (gender, age, facequality)
  ├─ checks: exactly 1 face · facequality ≥ 0.30 · gender known
  └─ ADMIN write → Users/{uid}/verification {faceVerified, gender, confidence,
     verifiedAt, source:"faceplusplus"}  + system notification row
        ↓
Profile badge (read-only for clients — rules deny client writes)
```

## One-time setup (≈ 10 minutes)

1. **Blaze plan** — required for Cloud Functions. *Already enabled on this project.*

2. **Face++ keys** — sign up at <https://console.faceplusplus.com> (free tier:
   ≈ 30,000 API calls/month). Copy the API Key + API Secret.

3. **Store the secrets** (from the repo root):

   ```bash
   npm install -g firebase-tools
   firebase login
   firebase use <your-firebase-project-id>
   firebase functions:secrets:set FACEPP_KEY
   firebase functions:secrets:set FACEPP_SECRET
   ```

4. **Deploy the function + the hardened RTDB rules:**

   ```bash
   cd functions && npm install && cd ..
   firebase deploy --only functions,database
   ```

## Verify it works

```bash
# Sanity: Face++ key works (should return faces: [])
curl -s -X POST "https://api-us.faceplusplus.com/facepp/v3/detect" \
  -d "api_key=$FACEPP_KEY" -d "api_secret=$FACEPP_SECRET" \
  -d "image_url=https://faceplusplus.com/static/img/demo/1.jpg"
```

Then in the app: Profile → Verification → run the liveness challenge.
On success the badge appears and a system notification lands in the
Notifications tab. Check logs with `firebase functions:log`.

## Error tokens (mapped to in-app messages)

| Token | Meaning |
|---|---|
| `VERIFY_NOT_CONFIGURED` | Secrets not set / not deployed yet |
| `VERIFY_NO_FACE` | No single good-quality face in the frame |
| `VERIFY_BAD_IMAGE` | Payload too small / too large |
| `VERIFY_UPSTREAM_FAILED` | Face++ unreachable — retry shortly |
| `VERIFY_COOLDOWN` | Face++ quota / rate limit hit |

## Why this is tamper-proof

- RTDB rules now contain `Users/$uid/verification { ".write": false }` — no client
  (even a modified APK) can set the badge; the Admin SDK in the function bypasses rules,
  and it writes **only** after Face++ confirms a real face.
- The function requires Firebase Auth; every call is attributable to a uid.
- Liveness (blink + smile) happens before upload, so a static photo passes only
  with difficulty; Face++ `facequality` adds a second gate server-side.

## Notes

- The on-device TFLite gender result is now only a **pre-gate** (UX + quota saver);
  the authoritative gender/confidence comes from Face++.
- Re-verification within 24 h returns the cached verdict at zero cost.
- Face++ free tier ≈ 30k calls/month; the cache + pre-gates keep usage far below that.
