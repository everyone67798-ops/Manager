package com.example.unitvmanager.data.model

enum class MessageType {
    INFO,
    SUCCESS,
    ERROR
}

/**
 * Model for user notification alerts and snackbars.
 */
data class UserMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val type: MessageType = MessageType.INFO
)
