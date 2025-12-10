package com.example.access_control.viewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CardReaderViewModel {
    data class DialogState(
        val showDialog: Boolean = false,
        val message: String = ""
    )

    // Private mutable state - only this viewModel can change it
    private val _dialogState = MutableStateFlow(DialogState())
    // Public read-only state - UI can observe but not modify
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    fun showCardTappedDialog() {
        _dialogState.value = DialogState(
            showDialog = true,
            message = "Card tapped"
        )
    }

    fun hideDialog() {
        _dialogState.value = DialogState(
            showDialog = false,
            message = ""
        )
    }
}