package com.triggerapp.core.strings

/**
 * Root namespace for all Trigger string constants used across modules.
 * @author udit
 */
object TriggerStrings {

    /**
     * Compose NavHost route ids, path patterns, and nav argument names for type-safe linking.
     * @author udit
     */
    object Nav {
        const val SPLASH = "splash"
        const val LOGIN = "login"
        const val REGISTER = "register"
        const val FORGOT = "forgot"
        const val HOME = "home"
        const val CHAT_PREFIX = "chat"
        const val CHAT_PATTERN = "$CHAT_PREFIX/{peerId}"
        const val ARG_PEER_ID = "peerId"
        const val PEER_PROFILE_PREFIX = "peerProfile"
        const val PEER_PROFILE_PATTERN = "$PEER_PROFILE_PREFIX/{profileUserId}"
        const val ARG_PROFILE_USER_ID = "profileUserId"
        const val OTP = "otp"
        const val ARG_OTP_EMAIL = "otpEmail"
        const val ARG_OTP_PURPOSE = "otpPurpose"
        const val NEW_PASSWORD = "newPassword"
        const val EDIT_PROFILE = "editProfile"
        const val VERIFY_PROFILE = "verifyProfile"
    }

    /**
     * Screen titles, button labels, hints, and accessibility-oriented copy for primary flows.
     * @author udit
     */
    object Ui {
        const val APP_NAME = "Trigger App"
        const val LOGIN = "Login"
        const val REGISTER = "Register"
        const val SIGN_UP = "Sign up"
        const val EMAIL = "Email"
        const val PASSWORD = "Password"
        const val USERNAME = "Username"
        const val SIGN_UP_HERE = "Sign up here."
        const val ALREADY_HAVE_AN_ACCOUNT = "Already have an account?"
        const val LOGIN_HERE = " Login."
        const val DONT_HAVE_AN_ACCOUNT = "Don't have an account?"
        const val LOG_OUT = "Log out"
        const val CHATS = "Chats"
        const val USERS = "Users"
        const val PROFILE = "Profile"
        const val NAME = "Name"
        const val SEEN = "Seen"
        const val SEARCH = "Search…"
        const val TYPE_MESSAGE = "Type a message"
        const val SEND = "Send"
        const val NO_CHATS_YET = "All your chats will appear here!"
        const val PROFILE_NOT_AVAILABLE = "No profile data found for this account."
        const val BIO = "Bio"
        const val ABOUT = "About"
        const val PROFILE_PHOTO_HINT = "Tap your photo to add or change it."
        const val EDIT_PROFILE = "Edit profile"
        const val VERIFY_PROFILE = "Account verification"
        const val VERIFY_PROFILE_CTA = "Verify your face"
        const val VERIFIED_BADGE = "Verified"
        const val NOT_VERIFIED = "Not verified"
        const val VERIFY_INTRO_TITLE = "Face verification"
        const val VERIFY_INTRO_BODY =
            "Confirm you are a real person. We look for your face on camera and check a quick blink and smile — nothing is uploaded; the check runs on your phone."
        const val VERIFY_START = "Start verification"
        const val VERIFY_REDO = "Re-verify"
        const val VERIFY_PROMPT_CENTER = "Center your face in the circle"
        const val VERIFY_PROMPT_BLINK = "Now blink once"
        const val VERIFY_PROMPT_SMILE = "Great — now smile"
        const val VERIFY_ANALYZING = "Checking…"
        const val VERIFY_SUCCESS_TITLE = "You're verified!"
        const val VERIFY_SUCCESS_BODY = "Your profile now shows the verified badge."
        const val VERIFY_DETECTED_GENDER = "Detected"
        const val VERIFY_MULTIPLE_FACES = "Make sure you are alone in the frame."
        const val VERIFY_DONE = "Done"
        const val VERIFY_STATUS_VERIFIED = "Face verified on-device"
        const val GENDER = "Gender"
        const val DATE_OF_BIRTH = "Date of birth"
        const val DOB_HINT = "DD/MM/YYYY"
        const val GENDER_MALE = "Male"
        const val GENDER_FEMALE = "Female"
        const val GENDER_OTHER = "Other"
        const val GENDER_UNDISCLOSED = "Prefer not to say"
        const val PROFILE_PHOTO_VIEW_HINT = "Tap the photo to view it."
        const val SAVE = "Save"
        const val EDIT_USERNAME = "Edit username"
        const val EDIT_BIO = "Edit bio"
        const val CHANGE_PHOTO = "Change photo"
        const val TAKE_PHOTO = "Take photo"
        const val CHOOSE_FROM_GALLERY = "Choose from gallery"
        const val CANCEL = "Cancel"
        const val RESET_EMAIL_HINT = "Enter your email"
        const val SEND_RESET = "Send reset link"
        const val OTP_TITLE = "Verify your email"
        const val OTP_SUBTITLE_SIGNUP = "We sent a 6-digit code to\u0020"
        const val OTP_SUBTITLE_RESET = "We sent a 6-digit reset code to\u0020"
        const val OTP_SUBTITLE_TAIL = ". Enter it below to continue."
        const val OTP_VERIFY = "Verify"
        const val OTP_VERIFYING = "Verifying…"
        const val OTP_RESEND = "Resend code"
        const val OTP_RESEND_IN = "Resend code in\u0020"
        const val OTP_RESEND_SECONDS = "s"
        const val OTP_SENDING = "Sending code…"
        const val OTP_CHANGE_EMAIL = "Wrong email? Go back"
        const val LAST_SEEN_PREFIX = "last seen\u0020"
        const val LAST_SEEN_TODAY = "today at\u0020"
        const val NEW_PASSWORD_TITLE = "Create a new password"
        const val NEW_PASSWORD_SUBTITLE = "Your new password must be different from previously used passwords."
        const val NEW_PASSWORD = "New password"
        const val CONFIRM_PASSWORD = "Confirm password"
        const val UPDATE_PASSWORD = "Update password"
        const val USERNAME_AVAILABLE = "Username is available"
        const val USERNAME_CHECKING = "Checking availability…"
        const val USERNAME_HINT = "6–25 characters · letters, numbers and _"
        const val VALIDATING = "Validating…"
        const val CREATING_ACCOUNT = "Creating your account…"
        const val UPDATING_PASSWORD = "Updating password…"
        const val BACK = "Back"
        const val TRY_AGAIN = "Try again"
        const val CLOSE = "Close"
        const val OFFLINE_INDICATOR =
            "You're offline. Chats and profile may not update until you reconnect."
        const val HI_THERE = "Hi there!"
        const val LOG_IN_TO_CONTINUE = "Log in to continue."
        const val WELCOME_SIGN = "Welcome!"
        const val FORGET_PASSWORD_LINE = "Forget password?\u0020"
        const val RESET_PASSWORD_TITLE = "Reset Password"
        const val SHOW_PASSWORD = "Show password"
        const val HIDE_PASSWORD = "Hide password"
        const val CONTACT_INFO = "Contact info"
        const val PROFILE_PHOTO = "Profile photo"
        const val SHARE = "Share"
        const val SAVE_TO_GALLERY = "Save to gallery"
        const val NO_PHOTO_TO_SHARE = "No profile photo to share or save."
        const val PHOTO_SAVED = "Photo saved to gallery"
        const val COULD_NOT_SAVE_PHOTO = "Couldn't save photo. Try again."
    }

    /**
     * Validation failures, auth errors, offline messaging, and Firebase setup guidance for Snack bars and dialogs.
     * @author udit
     */
    object Errors {
        const val GENERIC = "Something went wrong. Try again."
        const val FILL_EMAIL_PASSWORD = "Fill in email and password."
        const val SIGN_IN_FAILED = "Sign-in failed."
        const val ALL_FIELDS_REQUIRED = "All fields are required."
        const val REGISTRATION_FAILED = "Registration failed."
        const val ENTER_EMAIL = "Enter your email."
        const val REQUEST_FAILED = "Request failed."
        const val UPDATE_FAILED = "Update failed."
        const val INVALID_DOB = "Enter a valid date of birth (DD/MM/YYYY)"
        const val UPLOAD_FAILED = "Upload failed."
        const val NOT_SIGNED_IN = "Not signed in"
        const val NO_UID_AFTER_SIGNUP = "No UID after sign-up"
        const val FIREBASE_CONFIG_API_KEY =
            "Firebase is not set up: in Firebase Console add an Android app with package com.triggerapp.connect, " +
                "download google-services.json, and replace the file in app/."
        const val INVALID_EMAIL_OR_PASSWORD = "Incorrect email or password."
        const val INVALID_EMAIL = "That email address is not valid."
        const val EMAIL_INVALID_FORMAT = "Enter a valid email address."
        const val PASSWORD_MIN = "Password must be at least 6 characters."
        const val PASSWORDS_DO_NOT_MATCH = "Passwords do not match."
        const val USERNAME_REQUIRED = "Choose a username first."
        const val USERNAME_TOO_SHORT = "Username must be at least 6 characters."
        const val USERNAME_INVALID_RULES = "Use 6–25 characters: letters, numbers and _ only."
        const val USERNAME_TAKEN = "That username is already taken."
        const val USERNAME_UNCHANGED = "Pick a different username than your current one."
        const val OTP_INVALID = "That code is incorrect. Check the email and try again."
        const val OTP_EXPIRED = "That code has expired. Send a new one."
        const val OTP_TOO_MANY_ATTEMPTS = "Too many wrong attempts. Request a new code."
        const val OTP_COOLDOWN = "Please wait a minute before requesting a new code."
        const val OTP_SEND_FAILED = "Couldn't send the code. Try again."
        const val OTP_SERVICE_NOT_CONFIGURED =
            "Email verification isn't configured yet on the server. Contact the app owner."
        const val OTP_EMAIL_IN_USE = "That email is already registered. Try logging in."
        const val OTP_EMAIL_NOT_FOUND = "No account found with that email."
        const val EMAIL_ALREADY_IN_USE = "That email is already registered."
        const val WEAK_PASSWORD = "Password is too weak (use at least 6 characters)."
        const val USER_DISABLED = "This account has been disabled."
        const val TOO_MANY_ATTEMPTS = "Too many attempts. Try again later."
        const val OPERATION_NOT_ALLOWED =
            "Email/password sign-in is disabled. Enable it in Firebase Console → Authentication → Sign-in method."
        const val NETWORK_ERROR = "Network error. Check your connection."
        const val OFFLINE_GENERIC =
            "You're offline. Check your Wi-Fi or mobile data, then try again."
        const val CAMERA_PERMISSION_REQUIRED =
            "Allow camera access to take a new profile photo, or pick one from the gallery."
        const val OFFLINE_AUTH_SIGN_IN =
            "You're offline. Connect to the internet to sign in."
        const val OFFLINE_AUTH_SIGN_UP =
            "You're offline. Connect to the internet to create an account."
        const val OFFLINE_AUTH_RESET =
            "You're offline. Connect to the internet to send a reset link."
        const val LOAD_PROFILE_FAILED =
            "Couldn't load your profile. Check your connection and try again."
        const val LOAD_MESSAGES_FAILED =
            "Couldn't load messages. Check your connection and try again."
        const val LOAD_PEOPLE_FAILED =
            "Couldn't load people. Check your connection and try again."

        /**
         * Used when [com.triggerapp.core.common.errors.userFacingMessage] detects permission / unauthenticated
         * patterns in the exception chain (not raw Firebase text).
         * @author udit
         */
        const val DATA_ACCESS_DENIED =
            "Access denied. Sign out and sign in again, then try again."

        /**
         * Used when aggregated throwable messages suggest missing RTDB index / query setup (e.g. `indexOn` hints).
         * @author udit
         */
        const val LIST_TEMPORARILY_UNAVAILABLE =
            "This list couldn't be loaded. Try again in a moment."

        const val LOAD_CHATS_FAILED =
            "Couldn't load your chats. Check your connection and try again."
        const val MESSAGE_SEND_FAILED =
            "Message couldn't be sent. Check your connection and try again."
        const val PROFILE_SAVE_OFFLINE =
            "You're offline. Connect to the internet to save changes."
        const val PROFILE_PHOTO_OFFLINE =
            "You're offline. Connect to the internet to update your photo."
        const val VERIFICATION_FAILED =
            "Couldn't confirm your face this time. Find even lighting and try again."
        const val VERIFICATION_SAVE_FAILED =
            "Face check passed, but saving the result failed. Check your connection and try again."
        const val VERIFICATION_CAMERA_FAILED =
            "Couldn't open the camera. Close other camera apps and try again."
    }

    /**
     * Names of the HTTPS-callable Cloud Functions backing OTP flows and username renames.
     * @author udit
     */
    object Functions {
        const val SEND_OTP = "sendOtp"
        const val VERIFY_OTP = "verifyOtp"
        const val SIGN_UP_WITH_OTP = "signUpWithOtp"
        const val RESET_PASSWORD_WITH_OTP = "resetPasswordWithOtp"
        const val CHANGE_USERNAME = "changeUsername"
    }

    /**
     * Non-error user feedback such as post-action confirmations (e.g. email sent).
     * @author udit
     */
    object Messages {
        const val RESET_EMAIL_SENT = "Check your inbox for reset instructions."
        const val OTP_SENT = "Verification code sent to your email."
        const val PASSWORD_RESET_SUCCESS = "Password updated — you're signed in!"
        const val USERNAME_UPDATED = "Username updated."
    }

    /**
     * Firebase Realtime Database node names, child field keys, and query helpers shared by data/domain layers.
     * @author udit
     */
    object Db {
        const val NODE_USERS = "Users"
        const val NODE_CHATS = "Chats"
        const val NODE_CHAT_LIST = "ChatList"
        const val NODE_TOKENS = "Tokens"
        const val NODE_OTPS = "Otps"
        const val NODE_USERNAMES = "Usernames"

        const val CHILD_ID = "id"
        const val CHILD_USERNAME = "username"
        const val CHILD_EMAIL_ID = "emailId"
        const val CHILD_TIMESTAMP = "timestamp"
        const val CHILD_IMAGE_URL = "imageUrl"
        const val CHILD_BIO = "bio"
        const val CHILD_STATUS = "status"
        const val CHILD_SEARCH = "search"
        const val CHILD_SENDER_ID = "senderId"
        const val CHILD_RECEIVER_ID = "receiverId"
        const val CHILD_MESSAGE = "message"
        const val CHILD_SEEN = "seen"
        const val CHILD_TOKEN = "token"
        const val CHILD_LAST_SEEN = "lastSeen"
        const val CHILD_DISPLAY_NAME = "displayName"
        const val CHILD_GENDER = "gender"
        const val CHILD_DOB = "dob"
        const val NODE_VERIFICATION = "verification"
        const val CHILD_FACE_VERIFIED = "faceVerified"
        const val CHILD_DETECTED_GENDER = "gender"
        const val CHILD_CONFIDENCE = "confidence"
        const val CHILD_VERIFIED_AT = "verifiedAt"

        const val ORDER_BY_USERNAME = "username"
        const val ORDER_BY_SEARCH = "search"
        const val SEARCH_SUFFIX_HIGH = "\uf8ff"
    }

    /**
     * Fallback profile image token, starter bio, offline status, and online presence token used by domain rules.
     * @author udit
     */
    object Defaults {
        const val PROFILE_IMAGE = "default"
        const val NEW_USER_BIO = "Hey there!"
        const val STATUS_OFFLINE = "offline"
        const val PRESENCE_ONLINE = "online"
    }

    /**
     * SharedPreferences file name, keys, and sentinel values for lightweight local state (e.g. open chat).
     * @author udit
     */
    object Prefs {
        const val FILE_NAME = "PREFS"
        const val KEY_CURRENT_USER = "current"
        const val VALUE_NO_ACTIVE_CHAT = "none"
    }

    /**
     * FCM notification title and payload keys exchanged between the client, backend, and push handling code.
     * @author udit
     */
    object Fcm {
        const val NOTIFICATION_TITLE = "New Message"
        const val USER = "user"
        const val BODY = "body"
        const val TITLE = "title"
        const val SENT = "sent"
    }

    /**
     * File extensions and MIME-related hints for profile image compression and export pipelines.
     * @author udit
     */
    object Media {
        const val JPEG_EXTENSION = "jpg"
    }

    /**
     * Android notification channel id and display name for message notifications.
     * @author udit
     */
    object Notification {
        const val CHANNEL_ID_MESSAGES = "trigger_messages"
        const val CHANNEL_NAME_MESSAGES = "Messages"
    }
}
