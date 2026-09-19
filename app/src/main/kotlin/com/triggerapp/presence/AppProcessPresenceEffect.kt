package com.triggerapp.presence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.presence.SetUserPresenceUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Syncs Realtime Database presence with **app foreground** using [ProcessLifecycleOwner]:
 * online on [Lifecycle.Event.ON_START], offline on [Lifecycle.Event.ON_STOP].
 *
 * This runs for the whole process (not per Composable destination), so status updates as soon as
 * the user leaves the app (home/recents/another app). [SetUserPresenceUseCase] no-ops when signed out.
 */
@Composable
fun AppProcessPresenceEffect() {
    val setPresence: SetUserPresenceUseCase = koinInject()
    val scope = rememberCoroutineScope()
    DisposableEffect(setPresence) {
        val process = ProcessLifecycleOwner.get()
        fun syncFromCurrentState() {
            scope.launch {
                if (process.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    setPresence(TriggerStrings.Defaults.PRESENCE_ONLINE)
                } else {
                    setPresence(TriggerStrings.Defaults.STATUS_OFFLINE)
                }
            }
        }
        syncFromCurrentState()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    scope.launch {
                        setPresence(TriggerStrings.Defaults.PRESENCE_ONLINE)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    scope.launch {
                        setPresence(TriggerStrings.Defaults.STATUS_OFFLINE)
                    }
                }
                else -> {}
            }
        }
        process.lifecycle.addObserver(observer)
        onDispose {
            process.lifecycle.removeObserver(observer)
        }
    }
}
