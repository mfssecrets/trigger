package com.triggerapp.data.repository.firebase

import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.data.mapper.toUserOrNull
import com.triggerapp.domain.model.PagedUsersResult
import com.triggerapp.domain.model.User
import com.triggerapp.domain.model.UserListCursor
import com.triggerapp.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Whether this throwable chain indicates Realtime Database **permission denied** (read rejected).
 *
 * Used to skip the in-memory full-tree fallback in [UserRepositoryImpl.fetchUsersDirectoryPage], since that
 * path would fail for the same reason.
 *
 *
 * @receiver Any failure from an indexed or shallow `Users` read.
 * @return `true` if a cause message matches known permission-denied phrasing.
 * @author udit
 */
private fun Throwable.isFirebaseDatabasePermissionDenied(): Boolean {
    var t: Throwable? = this
    val seen = mutableSetOf<Throwable>()
    while (t != null && t !in seen) {
        seen.add(t)
        val m = t.message?.lowercase().orEmpty()
        if (m.contains("permission denied")) return true
        if (m.contains("permission_denied")) return true
        if (m.contains("client doesn't have permission")) return true
        t = t.cause
    }
    return false
}

/**
 * Koin-provided [UserRepository]: `Users` node I/O, search, presence, and base64 avatars.
 *
 * **Directory paging:** [fetchUsersDirectoryPage] prefers an indexed `orderByChild` query (`username` or `search`).
 * On any non-permission failure (e.g. missing `.indexOn` in rules), it falls back to a single `Users` snapshot
 * and paginates in memory with the same ordering semantics.
 * @author udit
 */
class UserRepositoryImpl(
    private val auth: FirebaseAuth,
    database: FirebaseDatabase,
    private val functions: FirebaseFunctions,
) : UserRepository {

    private val usersRef: DatabaseReference = database.reference.child(TriggerStrings.Db.NODE_USERS)

    /**
     * Observes `Users/{currentUid}` or completes with `null` when signed out.
     *
     *
     * @return Hot [Flow] of the signed-in profile.
     * @author udit
     */
    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val ref = usersRef.child(uid)
        val listener = listener(this::trySend)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Observes a single `Users/{userId}` document.
     *
     *
     * @param userId Profile key to watch.
     * @return Hot [Flow] of that user or `null` on cancel/error in listener.
     * @author udit
     */
    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val ref = usersRef.child(userId)
        val listener = listener(this::trySend)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Lists all users ordered by the `username` child for directory-style UIs.
     *
     *
     * @return Hot [Flow] of ordered [User] rows.
     * @author udit
     */
    override fun observeAllUsersOrdered(): Flow<List<User>> = callbackFlow {
        val ref = usersRef.orderByChild(TriggerStrings.Db.ORDER_BY_USERNAME)
        val listener = object : ValueEventListener {
            /**
             * Maps all children to domain users whenever data changes.
             *
             *
             * @param snapshot Ordered query snapshot under `Users`.
             * @author udit
             */
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.toUserOrNull() }
                trySend(list)
            }

            /**
             * Propagates read failures by closing the flow.
             *
             *
             * @param error Database error details.
             * @author udit
             */
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Blank prefix lists all users ordered by username; otherwise prefix-queries the `search` child.
     *
     *
     * @param prefix Case-sensitive prefix for keyed search, or blank for full list.
     * @return Hot [Flow] of matching users.
     * @author udit
     */
    override fun searchUsers(prefix: String): Flow<List<User>> = callbackFlow {
        if (prefix.isBlank()) {
            val ref = usersRef.orderByChild(TriggerStrings.Db.ORDER_BY_USERNAME)
            val listener = object : ValueEventListener {
                /**
                 *
                 * @param snapshot Full user list ordered by username.
                 * @author udit
                 */
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.children.mapNotNull { it.toUserOrNull() })
                }

                /**
                 *
                 * @param error Database error details.
                 * @author udit
                 */
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        } else {
            val ref = usersRef.orderByChild(TriggerStrings.Db.ORDER_BY_SEARCH)
                .startAt(prefix)
                .endAt(prefix + TriggerStrings.Db.SEARCH_SUFFIX_HIGH)
            val listener = object : ValueEventListener {
                /**
                 *
                 * @param snapshot Prefix range query snapshot on `search`.
                 * @author udit
                 */
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.children.mapNotNull { it.toUserOrNull() })
                }

                /**
                 *
                 * @param error Database error details.
                 * @author udit
                 */
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    /**
     * One page of the user directory: tries an indexed RTDB query first, then a full `Users` read + local paging.
     *
     *
     * @param prefix Lowercase prefix for `search` range, or blank for all users ordered by `username`.
     * @param pageSize Max rows to return; one extra row may be requested internally to set [PagedUsersResult.hasMore].
     * @param cursor Previous page boundary from [PagedUsersResult.nextCursor], or `null` for the first page.
     * @param excludeUserId Optional uid omitted from the returned page (typically the signed-in user).
     * @return Success with [PagedUsersResult], or failure if both indexed and fallback reads fail.
     *
     * **Cancellation:** [kotlinx.coroutines.CancellationException] is rethrown and not wrapped in [Result].
     *
     * @see fetchUsersDirectoryPageIndexed
     * @see fetchUsersDirectoryPageFullScan
     * @author udit
     */
    override suspend fun fetchUsersDirectoryPage(
        prefix: String,
        pageSize: Int,
        cursor: UserListCursor?,
        excludeUserId: String?,
    ): Result<PagedUsersResult> {
        val indexed = try {
            Result.success(fetchUsersDirectoryPageIndexed(prefix, pageSize, cursor, excludeUserId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
        if (indexed.isSuccess) return indexed
        val err = indexed.exceptionOrNull()!!
        if (err.isFirebaseDatabasePermissionDenied()) return Result.failure(err)
        return try {
            Result.success(fetchUsersDirectoryPageFullScan(prefix, pageSize, cursor, excludeUserId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Paged `Users` read using `orderByChild` + [com.google.firebase.database.Query.get] (requires matching `.indexOn` in rules).
     *
     *
     * @param prefix Blank → order by `username`; else range on `search`.
     * @param pageSize Positive page size (implementation uses `pageSize + 1` for overflow detection).
     * @param cursor Pagination token, or `null` for the first page.
     * @param excludeUserId Passed through to [finalizeDirectoryPage].
     * @return One slice plus [PagedUsersResult.hasMore] / [PagedUsersResult.nextCursor].
     * @author udit
     */
    private suspend fun fetchUsersDirectoryPageIndexed(
        prefix: String,
        pageSize: Int,
        cursor: UserListCursor?,
        excludeUserId: String?,
    ): PagedUsersResult {
        require(pageSize > 0) { "pageSize must be positive" }
        val limit = pageSize + 1
        val snap = if (prefix.isBlank()) {
            val base = usersRef.orderByChild(TriggerStrings.Db.ORDER_BY_USERNAME)
            val q = if (cursor == null) {
                base.limitToFirst(limit)
            } else {
                base.startAt(cursor.orderValue, cursor.childKey).limitToFirst(limit)
            }
            q.get().await()
        } else {
            val base = usersRef.orderByChild(TriggerStrings.Db.ORDER_BY_SEARCH)
                .startAt(prefix)
                .endAt(prefix + TriggerStrings.Db.SEARCH_SUFFIX_HIGH)
            val q = if (cursor == null) {
                base.limitToFirst(limit)
            } else {
                base.startAt(cursor.orderValue, cursor.childKey).limitToFirst(limit)
            }
            q.get().await()
        }
        var list = snap.children.mapNotNull { it.toUserOrNull() }
        if (cursor != null && list.isNotEmpty()) {
            val first = list.first()
            if (first.id == cursor.childKey) {
                val orderMatch = if (prefix.isBlank()) first.username else first.searchKey
                if (orderMatch == cursor.orderValue) {
                    list = list.drop(1)
                }
            }
        }
        return finalizeDirectoryPage(list, prefix, pageSize, excludeUserId)
    }

    /**
     * Fallback directory page: [usersRef.get] without `orderByChild`, then sort/filter/paginate in process memory.
     *
     * Avoids RTDB index requirements at the cost of downloading all user profiles for this request.
     *
     *
     * @param prefix Same semantics as [fetchUsersDirectoryPageIndexed].
     * @param pageSize Same as [fetchUsersDirectoryPageIndexed].
     * @param cursor Same as [fetchUsersDirectoryPageIndexed].
     * @param excludeUserId Same as [fetchUsersDirectoryPageIndexed].
     * @return Same shape as the indexed path via [finalizeDirectoryPage].
     * @author udit
     */
    private suspend fun fetchUsersDirectoryPageFullScan(
        prefix: String,
        pageSize: Int,
        cursor: UserListCursor?,
        excludeUserId: String?,
    ): PagedUsersResult {
        require(pageSize > 0) { "pageSize must be positive" }
        val limit = pageSize + 1
        val all = usersRef.get().await().children.mapNotNull { it.toUserOrNull() }
        var working = if (prefix.isBlank()) {
            all.sortedWith { a, b -> compareUsersDirectory(a, b, prefix) }
        } else {
            all.filter { it.searchKey.startsWith(prefix) }
                .sortedWith { a, b -> compareUsersDirectory(a, b, prefix) }
        }
        if (cursor != null) {
            val c = cursor
            working = working.filter { u ->
                val ord = userOrderKey(u, prefix)
                val cmp = ord.compareTo(c.orderValue)
                cmp > 0 || (cmp == 0 && u.id.compareTo(c.childKey) >= 0)
            }
            if (working.isNotEmpty()) {
                val first = working.first()
                if (first.id == c.childKey && userOrderKey(first, prefix) == c.orderValue) {
                    working = working.drop(1)
                }
            }
        }
        val windowed = working.take(limit)
        return finalizeDirectoryPage(windowed, prefix, pageSize, excludeUserId)
    }

    /**
     * Sort / pagination key: `username` for the full directory, else `search` for prefix mode.
     * @author udit
     */
    private fun userOrderKey(u: User, prefix: String): String =
        if (prefix.isBlank()) u.username else u.searchKey

    /**
     * Stable ascending order matching Firebase `orderByChild` + secondary key by user id.
     * @author udit
     */
    private fun compareUsersDirectory(a: User, b: User, prefix: String): Int {
        val o = userOrderKey(a, prefix).compareTo(userOrderKey(b, prefix))
        if (o != 0) return o
        return a.id.compareTo(b.id)
    }

    /**
     * Turns an ordered candidate list (length ≤ `pageSize + 1`) into [PagedUsersResult]: trims to a page,
     * drops [excludeUserId] from the visible page, and builds [UserListCursor] when more rows exist.
     *
     *
     * @param list Rows already in directory order (may include one overflow row for `hasMore`).
     * @param prefix Drives [userOrderKey] for the next cursor.
     * @param pageSize Requested page size (not including the optional overflow element).
     * @param excludeUserId Optional row filter applied after slicing the page.
     * @author udit
     */
    private fun finalizeDirectoryPage(
        list: List<User>,
        prefix: String,
        pageSize: Int,
        excludeUserId: String?,
    ): PagedUsersResult {
        val hasMore = list.size > pageSize
        val pageRaw = if (hasMore) list.take(pageSize) else list
        val page = excludeUserId?.let { ex -> pageRaw.filter { it.id != ex } } ?: pageRaw
        val next = if (hasMore && pageRaw.isNotEmpty()) {
            val last = pageRaw.last()
            UserListCursor(orderValue = userOrderKey(last, prefix), childKey = last.id)
        } else {
            null
        }
        return PagedUsersResult(users = page, nextCursor = next, hasMore = hasMore)
    }

    /**
     * Parallel [DatabaseReference.get] per id; preserves [ids] distinct order.
     * @author udit
     */
    override suspend fun fetchUsersByIds(ids: List<String>): Result<List<User>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyList()
        val order = ids.distinct()
        coroutineScope {
            val deferreds = order.map { id ->
                async { id to usersRef.child(id).get().await().toUserOrNull() }
            }
            val byId = deferreds.associate { it.await() }
            order.mapNotNull { byId[it] }
        }
    }

    /**
     * Writes the initial user map for a newly registered account.
     *
     *
     * @param userId New user UID.
     * @param username Display name.
     * @param email Email stored on the profile.
     * @param timestamp Registration timestamp string.
     * @param imageUrl Avatar placeholder or URL.
     * @return [Result] success when `setValue` completes.
     * @author udit
     */
    override suspend fun createUserProfile(
        userId: String,
        username: String,
        email: String,
        timestamp: String,
        imageUrl: String,
    ): Result<Unit> = runCatching {
        val map = mapOf(
            TriggerStrings.Db.CHILD_ID to userId,
            TriggerStrings.Db.CHILD_USERNAME to username,
            TriggerStrings.Db.CHILD_EMAIL_ID to email,
            TriggerStrings.Db.CHILD_TIMESTAMP to timestamp,
            TriggerStrings.Db.CHILD_IMAGE_URL to imageUrl,
            TriggerStrings.Db.CHILD_BIO to TriggerStrings.Defaults.NEW_USER_BIO,
            TriggerStrings.Db.CHILD_STATUS to TriggerStrings.Defaults.STATUS_OFFLINE,
            TriggerStrings.Db.CHILD_SEARCH to username.lowercase(),
        )
        usersRef.child(userId).setValue(map).await()
    }

    /**
     * Updates `username` and lowercase `search` for the signed-in user.
     *
     *
     * @param username New display name.
     * @return [Result] success when `updateChildren` completes.
     * @author udit
     */
    override suspend fun updateUsername(username: String): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
        // Server-enforced rename: validates the handle, re-claims the unique registry key,
        // and updates username/search atomically (client writes to those children are blocked by rules).
        functions.getHttpsCallable(TriggerStrings.Functions.CHANGE_USERNAME)
            .call(mapOf("username" to username))
            .await()
        Unit
    }

    /**
     * Sets the `bio` child for the signed-in user.
     *
     *
     * @param bio New bio text.
     * @return [Result] success when written.
     * @author udit
     */
    override suspend fun updateBio(bio: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
        usersRef.child(uid).child(TriggerStrings.Db.CHILD_BIO).setValue(bio).await()
    }

    /**
     * Writes simple profile fields (display name, gender, dob) in one multi-path update.
     *
     *
     * @param fields Child-name to value map (client-writable children only).
     * @return [Result] success when written.
     * @author udit
     */
    override suspend fun updateProfileFields(fields: Map<String, String>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
        usersRef.child(uid).updateChildren(fields).await()
        Unit
    }

    /**
     * Writes the on-device face-verification result in one multi-path update so the
     * `verification` node lands atomically (server timestamp from the backend clock).
     *
     *
     * @param gender Classifier label (`"male"` / `"female"`).
     * @param confidence Classifier confidence in `[0,1]`.
     * @return [Result] success when written.
     * @author udit
     */
    override suspend fun saveFaceVerification(gender: String, confidence: Double): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
            val node = TriggerStrings.Db.NODE_VERIFICATION
            usersRef.child(uid).updateChildren(
                mapOf(
                    "$node/${TriggerStrings.Db.CHILD_FACE_VERIFIED}" to true,
                    "$node/${TriggerStrings.Db.CHILD_DETECTED_GENDER}" to gender,
                    "$node/${TriggerStrings.Db.CHILD_CONFIDENCE}" to confidence,
                    "$node/${TriggerStrings.Db.CHILD_VERIFIED_AT}" to ServerValue.TIMESTAMP,
                ),
            ).await()
            Unit
        }

    /**
     * Sets `imageUrl` (HTTPS or data URI) for the signed-in user.
     *
     *
     * @param url Stored image reference string.
     * @return [Result] success when written.
     * @author udit
     */
    override suspend fun updateImageUrl(url: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
        usersRef.child(uid).child(TriggerStrings.Db.CHILD_IMAGE_URL).setValue(url).await()
    }

    /**
     * Updates `status` and registers onDisconnect offline when transitioning to online.
     *
     *
     * @param status Presence string written to the profile.
     * @return [Result] success or no-op when user node missing / not signed in.
     * @author udit
     */
    override suspend fun setPresence(status: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val snap = usersRef.child(uid).get().await()
        if (snap.exists()) {
            val profileRef = usersRef.child(uid)
            val statusRef = profileRef.child(TriggerStrings.Db.CHILD_STATUS)
            if (status.equals(TriggerStrings.Defaults.PRESENCE_ONLINE, ignoreCase = true)) {
                statusRef.setValue(TriggerStrings.Defaults.PRESENCE_ONLINE).await()
                statusRef.onDisconnect().setValue(TriggerStrings.Defaults.STATUS_OFFLINE).await()
                profileRef.child(TriggerStrings.Db.CHILD_LAST_SEEN)
                    .onDisconnect().setValue(ServerValue.TIMESTAMP).await()
            } else {
                profileRef.updateChildren(
                    mapOf(
                        TriggerStrings.Db.CHILD_STATUS to TriggerStrings.Defaults.STATUS_OFFLINE,
                        TriggerStrings.Db.CHILD_LAST_SEEN to ServerValue.TIMESTAMP,
                    ),
                ).await()
            }
        }
    }

    /**
     * Encodes [jpegBytes] as a base64 data URI and writes `imageUrl` for the signed-in user.
     *
     *
     * @param jpegBytes Compressed image bytes.
     * @param fileExtension File suffix driving MIME type (`png` vs jpeg family).
     * @return [Result] containing the persisted data URI string.
     * @author udit
     */
    override suspend fun uploadProfileImage(jpegBytes: ByteArray, fileExtension: String): Result<String> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
            val mime = when (fileExtension.lowercase()) {
                "png" -> "image/png"
                else -> "image/jpeg"
            }
            val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val dataUri = "data:$mime;base64,$b64"
            usersRef.child(uid).child(TriggerStrings.Db.CHILD_IMAGE_URL).setValue(dataUri).await()
            dataUri
        }

    /**
     * Builds a [ValueEventListener] that forwards mapped [User] rows or `null` on cancel.
     *
     *
     * @param emit Channel callback used by [callbackFlow].
     * @return Listener wired to a single `Users/{id}` ref.
     * @author udit
     */
    private fun listener(emit: (User?) -> Unit) = object : ValueEventListener {
        /**
         * Emits the mapped user for the watched node.
         *
         *
         * @param snapshot User document snapshot.
         * @author udit
         */
        override fun onDataChange(snapshot: DataSnapshot) {
            emit(snapshot.toUserOrNull())
        }

        /**
         * Emits `null` when the listener is canceled (no exception propagation here).
         *
         *
         * @param error Database error details.
         * @author udit
         */
        override fun onCancelled(error: DatabaseError) {
            emit(null)
        }
    }
}
