package com.triggerapp.data.repository.firebase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.model.SocialComment
import com.triggerapp.domain.model.SocialNotification
import com.triggerapp.domain.model.SocialPost
import com.triggerapp.domain.model.User
import com.triggerapp.domain.repository.SocialRepository
import com.triggerapp.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * Realtime Database [SocialRepository]: every feed / follow / notification action writes
 * to the prototype social nodes — no in-memory or seeded data anywhere.
 *
 * Like counters use [DatabaseReference.runTransaction] so concurrent likes don't clobber
 * each other. Notifications are written by the acting client (prototype-grade; a Cloud
 * Function trigger is the tamper-proof upgrade path).
 *
 * @author udit
 */
class SocialRepositoryImpl(
    private val auth: FirebaseAuth,
    database: FirebaseDatabase,
    private val userRepository: UserRepository,
) : SocialRepository {

    private val root: DatabaseReference = database.reference
    private val postsRef = root.child(TriggerStrings.Db.NODE_POSTS)
    private val postLikesRef = root.child(TriggerStrings.Db.NODE_POST_LIKES)
    private val userLikesRef = root.child(TriggerStrings.Db.NODE_USER_LIKES)
    private val postCommentsRef = root.child(TriggerStrings.Db.NODE_POST_COMMENTS)
    private val followersRef = root.child(TriggerStrings.Db.NODE_FOLLOWERS)
    private val followingRef = root.child(TriggerStrings.Db.NODE_FOLLOWING)
    private val notificationsRef = root.child(TriggerStrings.Db.NODE_NOTIFICATIONS)
    private val savedPostsRef = root.child(TriggerStrings.Db.NODE_SAVED_POSTS)
    private val reportsRef = root.child(TriggerStrings.Db.NODE_REPORTS)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)

    /** Loads the author's profile and denormalizes it into post/comment payloads. */
    private suspend fun authorPayload(): Map<String, Any?> {
        val uid = requireUid()
        val me: User? = userRepository.fetchUsersByIds(listOf(uid)).getOrNull()?.firstOrNull()
        return mapOf(
            TriggerStrings.Db.CHILD_AUTHOR_ID to uid,
            TriggerStrings.Db.CHILD_AUTHOR_NAME to (me?.effectiveDisplayName ?: "User"),
            TriggerStrings.Db.CHILD_AUTHOR_USERNAME to (me?.username ?: "user"),
            TriggerStrings.Db.CHILD_AUTHOR_AVATAR to (me?.imageUrl ?: TriggerStrings.Defaults.PROFILE_IMAGE),
            TriggerStrings.Db.CHILD_AUTHOR_VERIFIED to (me?.isFaceVerified ?: false),
        )
    }

    // ---- Feed ----

    override fun observeFeed(limit: Long): Flow<List<SocialPost>> = callbackFlow {
        val query = postsRef.orderByChild(TriggerStrings.Db.CHILD_CREATED_AT).limitToLast(limit.toInt())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { it.toSocialPostOrNull() }
                    .sortedByDescending { it.createdAt }
                trySend(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun createPost(
        content: String,
        tags: List<String>,
        jpegBytes: ByteArray?,
    ): Result<String> = runCatching {
        val trimmed = content.trim()
        require(trimmed.isNotEmpty()) { TriggerStrings.Errors.POST_EMPTY }
        val author = authorPayload()
        val newRef = postsRef.push()
        val postId = newRef.key ?: error(TriggerStrings.Errors.POST_CREATE_FAILED)
        val payload = buildMap {
            putAll(author)
            put(TriggerStrings.Db.CHILD_ID, postId)
            put(TriggerStrings.Db.CHILD_CONTENT, trimmed)
            put(TriggerStrings.Db.CHILD_TAGS, tags.joinToString(","))
            put(
                TriggerStrings.Db.CHILD_IMAGE_URI,
                jpegBytes?.let { toDataUri(it) } ?: "",
            )
            put(TriggerStrings.Db.CHILD_CREATED_AT, System.currentTimeMillis())
            put(TriggerStrings.Db.CHILD_LIKES_COUNT, 0L)
            put(TriggerStrings.Db.CHILD_COMMENTS_COUNT, 0L)
        }
        newRef.setValue(payload).await()
        postId
    }

    override suspend fun setPostLiked(postId: String, like: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        // One atomic multi-path update: like marks + denormalized counter delta
        // (ServerValue.increment is collision-safe under concurrent likes).
        val updates = mapOf<String, Any?>(
            "${TriggerStrings.Db.NODE_POST_LIKES}/$postId/$uid" to (if (like) true else null),
            "${TriggerStrings.Db.NODE_USER_LIKES}/$uid/$postId" to (if (like) true else null),
            "${TriggerStrings.Db.NODE_POSTS}/$postId/${TriggerStrings.Db.CHILD_LIKES_COUNT}" to
                ServerValue.increment(if (like) 1L else -1L),
        )
        root.updateChildren(updates).await()
        Unit
    }

    override fun observeLikedPostIds(): Flow<Set<String>> = callbackFlow {
        val uid = requireUid()
        val ref = userLikesRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key }.toSet())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeSavedPostIds(): Flow<Set<String>> = callbackFlow {
        val uid = requireUid()
        val ref = savedPostsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key }.toSet())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setPostSaved(postId: String, saved: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        savedPostsRef.child(uid).child(postId)
            .setValue(if (saved) System.currentTimeMillis() else null)
            .await()
        Unit
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val payload = mapOf(
            "reporterId" to uid,
            TriggerStrings.Db.CHILD_POST_ID to postId,
            TriggerStrings.Db.CHILD_REASON to reason,
            TriggerStrings.Db.CHILD_CREATED_AT to System.currentTimeMillis(),
        )
        reportsRef.push().setValue(payload).await()
        Unit
    }

    // ---- Comments ----

    override fun observeComments(postId: String): Flow<List<SocialComment>> = callbackFlow {
        val ref = postCommentsRef.child(postId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { it.toSocialCommentOrNull() }
                    .sortedBy { it.createdAt }
                trySend(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun addComment(postId: String, text: String): Result<Unit> = runCatching {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { TriggerStrings.Errors.GENERIC }
        val author = authorPayload()
        val postSnap = postsRef.child(postId).get().await()
        val postAuthorId = postSnap.child(TriggerStrings.Db.CHILD_AUTHOR_ID).getValue(String::class.java)

        val newRef = postCommentsRef.child(postId).push()
        val payload = buildMap {
            putAll(author)
            put(TriggerStrings.Db.CHILD_ID, newRef.key.orEmpty())
            put(TriggerStrings.Db.CHILD_TEXT, trimmed)
            put(TriggerStrings.Db.CHILD_CREATED_AT, System.currentTimeMillis())
        }
        // Comment + counter in one atomic multi-path update.
        val updates = mapOf<String, Any?>(
            "${TriggerStrings.Db.NODE_POST_COMMENTS}/$postId/${newRef.key}" to payload,
            "${TriggerStrings.Db.NODE_POSTS}/$postId/${TriggerStrings.Db.CHILD_COMMENTS_COUNT}" to
                ServerValue.increment(1L),
        )
        root.updateChildren(updates).await()

        val me = requireUid()
        if (postAuthorId != null && postAuthorId != me) {
            runCatching {
                pushNotification(
                    recipientId = postAuthorId,
                    type = TriggerStrings.NotificationTypes.COMMENT,
                    text = "commented on your post: \"$trimmed\"",
                    postId = postId,
                )
            }
        }
        Unit
    }

    // ---- Follows ----

    override fun observeIsFollowing(peerId: String): Flow<Boolean> = callbackFlow {
        val uid = requireUid()
        val ref = followingRef.child(uid).child(peerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observePeerFollowsMe(peerId: String): Flow<Boolean> = callbackFlow {
        val uid = requireUid()
        val ref = followingRef.child(peerId).child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeFollowerIds(userId: String): Flow<List<String>> = callbackFlow {
        val ref = followersRef.child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeFollowingIds(userId: String): Flow<List<String>> = callbackFlow {
        val ref = followingRef.child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setFollowing(peerId: String, follow: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        val updates = mapOf<String, Any?>(
            "${TriggerStrings.Db.NODE_FOLLOWING}/$uid/$peerId" to (if (follow) true else null),
            "${TriggerStrings.Db.NODE_FOLLOWERS}/$peerId/$uid" to (if (follow) true else null),
        )
        root.updateChildren(updates).await()
        if (follow && peerId != uid) {
            runCatching {
                val me = userRepository.fetchUsersByIds(listOf(uid)).getOrNull()?.firstOrNull()
                pushNotification(
                    recipientId = peerId,
                    type = TriggerStrings.NotificationTypes.FOLLOW,
                    text = "started following you",
                    actorId = uid,
                    actorName = me?.effectiveDisplayName ?: "Someone",
                    actorAvatar = me?.imageUrl ?: "",
                )
            }
        }
        Unit
    }

    // ---- Notifications ----

    override fun observeNotifications(): Flow<List<SocialNotification>> = callbackFlow {
        val uid = requireUid()
        val query = notificationsRef.child(uid)
            .orderByChild(TriggerStrings.Db.CHILD_CREATED_AT)
            .limitToLast(TriggerStrings.Db.NOTIFICATIONS_PAGE_SIZE.toInt())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rows = snapshot.children.mapNotNull { it.toSocialNotificationOrNull() }
                    .sortedByDescending { it.createdAt }
                trySend(rows)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> = runCatching {
        val uid = requireUid()
        val snap = notificationsRef.child(uid).get().await()
        if (!snap.exists()) return@runCatching
        val updates = snap.children
            .filterNot {
                it.child(TriggerStrings.Db.CHILD_READ).getValue(Boolean::class.java) == true
            }
            .mapNotNull { child ->
                child.key?.let {
                    "${TriggerStrings.Db.NODE_NOTIFICATIONS}/$uid/$it/${TriggerStrings.Db.CHILD_READ}" to true
                }
            }.toMap()
        if (updates.isNotEmpty()) root.updateChildren(updates).await()
        Unit
    }

    override suspend fun pushNotification(
        recipientId: String,
        type: String,
        text: String,
        actorId: String,
        actorName: String,
        actorAvatar: String,
        postId: String,
    ): Result<Unit> = runCatching {
        val uid = requireUid()
        val payload = mapOf(
            TriggerStrings.Db.CHILD_TYPE to type,
            TriggerStrings.Db.CHILD_ACTOR_ID to actorId.ifBlank { uid },
            TriggerStrings.Db.CHILD_ACTOR_NAME to actorName,
            TriggerStrings.Db.CHILD_ACTOR_AVATAR to actorAvatar,
            TriggerStrings.Db.CHILD_POST_ID to postId,
            TriggerStrings.Db.CHILD_TEXT to text,
            TriggerStrings.Db.CHILD_CREATED_AT to System.currentTimeMillis(),
            TriggerStrings.Db.CHILD_READ to false,
        )
        notificationsRef.child(recipientId).push().setValue(payload).await()
        Unit
    }

    // ---- Helpers ----

    /** Compresses raw JPEG bytes further if huge and encodes as a data URI. */
    private fun toDataUri(bytes: ByteArray): String {
        val capped = if (bytes.size > MAX_UPLOAD_BYTES) {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error(TriggerStrings.Errors.POST_CREATE_FAILED)
            val scaled = scaleDown(bmp, MAX_UPLOAD_DIMENSION)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, POST_IMAGE_QUALITY, out)
            out.toByteArray()
        } else {
            bytes
        }
        val b64 = Base64.encodeToString(capped, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        /** Hard cap before rescaling (keeps the RTDB row small enough for a data URI). */
        const val MAX_UPLOAD_BYTES = 500_000

        /** Longest edge after rescaling. */
        const val MAX_UPLOAD_DIMENSION = 1080

        /** JPEG quality used when rescaling oversized attachments. */
        const val POST_IMAGE_QUALITY = 78
    }
}

/**
 * Maps a `Posts/{id}` snapshot to a [SocialPost], or null when malformed.
 * @receiver Post snapshot.
 * @author udit
 */
internal fun DataSnapshot.toSocialPostOrNull(): SocialPost? {
    val id = child(TriggerStrings.Db.CHILD_ID).getValue(String::class.java) ?: key ?: return null
    val content = child(TriggerStrings.Db.CHILD_CONTENT).getValue(String::class.java) ?: return null
    val rawTags = child(TriggerStrings.Db.CHILD_TAGS).getValue(String::class.java).orEmpty()
    return SocialPost(
        id = id,
        authorId = child(TriggerStrings.Db.CHILD_AUTHOR_ID).getValue(String::class.java).orEmpty(),
        authorName = child(TriggerStrings.Db.CHILD_AUTHOR_NAME).getValue(String::class.java).orEmpty(),
        authorUsername = child(TriggerStrings.Db.CHILD_AUTHOR_USERNAME).getValue(String::class.java).orEmpty(),
        authorAvatar = child(TriggerStrings.Db.CHILD_AUTHOR_AVATAR).getValue(String::class.java).orEmpty(),
        authorVerified = child(TriggerStrings.Db.CHILD_AUTHOR_VERIFIED).getValue(Boolean::class.java) ?: false,
        content = content,
        tags = rawTags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        imageUri = child(TriggerStrings.Db.CHILD_IMAGE_URI).getValue(String::class.java)
            ?.takeUnless { it.isBlank() },
        createdAt = child(TriggerStrings.Db.CHILD_CREATED_AT).getValue(Long::class.java) ?: 0L,
        likesCount = child(TriggerStrings.Db.CHILD_LIKES_COUNT).getValue(Long::class.java) ?: 0L,
        commentsCount = child(TriggerStrings.Db.CHILD_COMMENTS_COUNT).getValue(Long::class.java) ?: 0L,
    )
}

/**
 * Maps a `PostComments/{postId}/{id}` snapshot to a [SocialComment], or null when malformed.
 * @receiver Comment snapshot.
 * @author udit
 */
internal fun DataSnapshot.toSocialCommentOrNull(): SocialComment? {
    val id = child(TriggerStrings.Db.CHILD_ID).getValue(String::class.java) ?: key ?: return null
    val text = child(TriggerStrings.Db.CHILD_TEXT).getValue(String::class.java) ?: return null
    return SocialComment(
        id = id,
        authorId = child(TriggerStrings.Db.CHILD_AUTHOR_ID).getValue(String::class.java).orEmpty(),
        authorName = child(TriggerStrings.Db.CHILD_AUTHOR_NAME).getValue(String::class.java).orEmpty(),
        authorUsername = child(TriggerStrings.Db.CHILD_AUTHOR_USERNAME).getValue(String::class.java).orEmpty(),
        authorAvatar = child(TriggerStrings.Db.CHILD_AUTHOR_AVATAR).getValue(String::class.java).orEmpty(),
        text = text,
        createdAt = child(TriggerStrings.Db.CHILD_CREATED_AT).getValue(Long::class.java) ?: 0L,
    )
}

/**
 * Maps a `Notifications/{uid}/{id}` snapshot to a [SocialNotification], or null when malformed.
 * @receiver Notification snapshot.
 * @author udit
 */
internal fun DataSnapshot.toSocialNotificationOrNull(): SocialNotification? {
    val id = key ?: return null
    val text = child(TriggerStrings.Db.CHILD_TEXT).getValue(String::class.java) ?: return null
    return SocialNotification(
        id = id,
        type = child(TriggerStrings.Db.CHILD_TYPE).getValue(String::class.java)
            ?: TriggerStrings.NotificationTypes.SYSTEM,
        actorId = child(TriggerStrings.Db.CHILD_ACTOR_ID).getValue(String::class.java).orEmpty(),
        actorName = child(TriggerStrings.Db.CHILD_ACTOR_NAME).getValue(String::class.java).orEmpty(),
        actorAvatar = child(TriggerStrings.Db.CHILD_ACTOR_AVATAR).getValue(String::class.java).orEmpty(),
        postId = child(TriggerStrings.Db.CHILD_POST_ID).getValue(String::class.java).orEmpty(),
        text = text,
        createdAt = child(TriggerStrings.Db.CHILD_CREATED_AT).getValue(Long::class.java) ?: 0L,
        read = child(TriggerStrings.Db.CHILD_READ).getValue(Boolean::class.java) ?: false,
    )
}
