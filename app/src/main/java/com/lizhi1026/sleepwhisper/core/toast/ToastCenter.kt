package com.lizhi1026.sleepwhisper.core.toast

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized one-shot event bus for transient toasts (port of iOS ToastCenter).
 *
 * AppStateContainer emits via [show]; a single ToastOverlay composable observes [events]
 * and displays them. The LiveData holds the **latest** event only — newer ones replace
 * older. Use [consume] from the UI after rendering to clear.
 */
@Singleton
class ToastCenter @Inject constructor() {

    enum class Style { INFO, SUCCESS, WARNING, ERROR }

    data class ToastItem(
        @StringRes val messageRes: Int,
        val arg: String? = null,
        val style: Style = Style.INFO,
        val undo: (() -> Unit)? = null,
        val editAction: (() -> Unit)? = null
    )

    private val _events = MutableLiveData<ToastItem?>(null)
    val events: LiveData<ToastItem?> get() = _events

    fun show(
        @StringRes messageRes: Int,
        arg: String? = null,
        style: Style = Style.INFO,
        undo: (() -> Unit)? = null,
        editAction: (() -> Unit)? = null
    ) {
        _events.postValue(ToastItem(messageRes, arg, style, undo, editAction))
    }

    /** Call from UI after rendering — clears the slot so re-renders don't repeat. */
    fun consume() {
        _events.postValue(null)
    }
}
