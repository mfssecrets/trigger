package com.triggerapp.data.local

import android.content.Context
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.repository.OpenChatTracker
import androidx.core.content.edit

/**
 * Koin-provided [OpenChatTracker]: stores the foreground chat peer id in app SharedPreferences.
 *
 *
 * @param context Application context from [org.koin.android.ext.koin.androidContext].
 * @author udit
 */
class OpenChatStore(
    context: Context,
) : OpenChatTracker {
    private val sp = context.getSharedPreferences(TriggerStrings.Prefs.FILE_NAME, Context.MODE_PRIVATE)

    /**
     * Persists the active peer id (or sentinel when cleared) for notification routing.
     *
     *
     * @param peerId Peer UID, or `null` when no chat is open.
     * @author udit
     */
    override fun setActivePeer(peerId: String?) {
        sp.edit {
            putString(
                TriggerStrings.Prefs.KEY_CURRENT_USER,
                peerId ?: TriggerStrings.Prefs.VALUE_NO_ACTIVE_CHAT,
            )
        }
    }

    /**
     * Reads the stored foreground peer id, if any.
     *
     *
     * @return Active peer UID, or `null` when none / sentinel value.
     * @author udit
     */
    override fun getOpenChatUserId(): String? {
        val v = sp.getString(TriggerStrings.Prefs.KEY_CURRENT_USER, TriggerStrings.Prefs.VALUE_NO_ACTIVE_CHAT)
            ?: TriggerStrings.Prefs.VALUE_NO_ACTIVE_CHAT
        return if (v == TriggerStrings.Prefs.VALUE_NO_ACTIVE_CHAT) null else v
    }
}
