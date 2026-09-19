package com.triggerapp.feature.home.presentation.feed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.social.AddPostCommentUseCase
import com.triggerapp.domain.usecase.social.CreatePostUseCase
import com.triggerapp.domain.usecase.social.ObserveFeedUseCase
import com.triggerapp.domain.usecase.social.ObserveLikedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ObservePostCommentsUseCase
import com.triggerapp.domain.usecase.social.ObserveSavedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ReportPostUseCase
import com.triggerapp.domain.usecase.social.SetPostLikedUseCase
import com.triggerapp.domain.usecase.social.SetPostSavedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * RTDB-backed feed [ViewModel]: streams posts + the user's like/save marks, and runs
 * every feed mutation (publish, like, save, report, comment) through the social use
 * cases. Nothing here is seeded or local-only — an empty feed means no posts exist.
 *
 * @param context App context (used to decode a picked image into compressed JPEG bytes).
 * @param observeFeed Streams feed posts.
 * @param observeLikedPostIds Streams the current user's liked post ids.
 * @param observeSavedPostIds Streams the current user's saved post ids.
 * @param createPost Publishes posts.
 * @param setPostLiked Flips like state.
 * @param setPostSaved Flips save state.
 * @param reportPost Files moderation reports.
 * @param observePostComments Streams one post's comments.
 * @param addPostComment Adds a comment.
 * @author udit
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val context: Context,
    observeFeed: ObserveFeedUseCase,
    observeLikedPostIds: ObserveLikedPostIdsUseCase,
    observeSavedPostIds: ObserveSavedPostIdsUseCase,
    private val createPost: CreatePostUseCase,
    private val setPostLiked: SetPostLikedUseCase,
    private val setPostSaved: SetPostSavedUseCase,
    private val reportPost: ReportPostUseCase,
    private val observePostComments: ObservePostCommentsUseCase,
    private val addPostComment: AddPostCommentUseCase,
) : ViewModel() {

    private val _extraState = MutableStateFlow(FeedExtraState())
    private val _effects = Channel<FeedUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val commentsFlow = _extraState.flatMapLatest { extra ->
        extra.selectedCommentPostId?.let { observePostComments(it) } ?: flowOf(emptyList())
    }

    /** Combined feed state snapshot; every source is a live RTDB stream. */
    val state: StateFlow<FeedUiState> = combine(
        observeFeed(TriggerStrings.Db.FEED_PAGE_SIZE).catch { e ->
            _extraState.update {
                it.copy(
                    isLoading = false,
                    loadError = e.userFacingMessage(
                        offlineFallback = TriggerStrings.Errors.FEED_LOAD_FAILED,
                        genericFallback = TriggerStrings.Errors.FEED_LOAD_FAILED,
                    ),
                )
            }
            emit(emptyList())
        },
        observeLikedPostIds().catch { emit(emptySet()) },
        observeSavedPostIds().catch { emit(emptySet()) },
        commentsFlow,
        _extraState,
    ) { posts, liked, saved, comments, extra ->
        FeedUiState(
            posts = posts,
            likedPostIds = liked,
            savedPostIds = saved,
            hiddenPostIds = extra.hiddenPostIds,
            isLoading = extra.isLoading && posts.isEmpty(),
            isPublishing = extra.isPublishing,
            loadError = extra.loadError,
            actionError = extra.actionError,
            selectedCommentPostId = extra.selectedCommentPostId,
            comments = comments,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    /**
     *
     * @param event Feed intent.
     * @author udit
     */
    fun onEvent(event: FeedUiEvent) {
        when (event) {
            FeedUiEvent.Retry -> _extraState.update {
                it.copy(isLoading = true, loadError = null)
            }
            is FeedUiEvent.ToggleLike -> viewModelScope.launch {
                val liked = state.value.likedPostIds.contains(event.postId)
                setPostLiked(event.postId, !liked).onFailure { e ->
                    surfaceActionError(e)
                }
            }
            is FeedUiEvent.ToggleSave -> viewModelScope.launch {
                val saved = state.value.savedPostIds.contains(event.postId)
                setPostSaved(event.postId, !saved).onFailure { e ->
                    surfaceActionError(e)
                }
            }
            is FeedUiEvent.HidePost -> _extraState.update {
                it.copy(hiddenPostIds = it.hiddenPostIds + event.postId)
            }
            is FeedUiEvent.ReportPost -> viewModelScope.launch {
                reportPost(event.postId, event.reason).fold(
                    onSuccess = { _effects.trySend(FeedUiEffect.ReportSubmitted) },
                    onFailure = { e -> surfaceActionError(e) },
                )
            }
            is FeedUiEvent.PublishPost -> publish(event)
            is FeedUiEvent.OpenComments -> _extraState.update {
                it.copy(selectedCommentPostId = event.postId)
            }
            FeedUiEvent.CloseComments -> _extraState.update {
                it.copy(selectedCommentPostId = null)
            }
            is FeedUiEvent.AddComment -> viewModelScope.launch {
                addPostComment(event.postId, event.text).fold(
                    onSuccess = { _effects.trySend(FeedUiEffect.CommentAdded) },
                    onFailure = { e -> surfaceActionError(e) },
                )
            }
            FeedUiEvent.ConsumeActionError -> _extraState.update { it.copy(actionError = null) }
        }
    }

    private fun publish(event: FeedUiEvent.PublishPost) {
        if (event.content.isBlank()) {
            _extraState.update { it.copy(actionError = TriggerStrings.Errors.POST_EMPTY) }
            return
        }
        _extraState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                event.imageUri?.let { loadCompressedJpeg(it) }
            }
            createPost(event.content.trim(), event.tags, bytes).fold(
                onSuccess = {
                    _extraState.update { it.copy(isPublishing = false) }
                    _effects.trySend(FeedUiEffect.PostPublished)
                },
                onFailure = { e ->
                    _extraState.update {
                        it.copy(
                            isPublishing = false,
                            actionError = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.POST_CREATE_FAILED,
                                genericFallback = TriggerStrings.Errors.POST_CREATE_FAILED,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun surfaceActionError(e: Throwable) {
        _extraState.update {
            it.copy(
                actionError = e.userFacingMessage(
                    offlineFallback = TriggerStrings.Errors.OFFLINE_GENERIC,
                    genericFallback = TriggerStrings.Errors.GENERIC,
                ),
            )
        }
    }

    /** Decodes a picked image Uri and compresses it to transfer-size-safe JPEG bytes. */
    private fun loadCompressedJpeg(uriString: String): ByteArray? = runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return@runCatching null
        val scaled = scaleDown(bitmap)
        val output = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        output.toByteArray()
    }.getOrNull()

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private data class FeedExtraState(
        val hiddenPostIds: Set<String> = emptySet(),
        val isLoading: Boolean = true,
        val isPublishing: Boolean = false,
        val loadError: String? = null,
        val actionError: String? = null,
        val selectedCommentPostId: String? = null,
    )

    private companion object {
        const val MAX_DIMENSION = 1080
        const val JPEG_QUALITY = 80
    }
}
