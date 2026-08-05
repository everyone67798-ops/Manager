package com.example.unitvmanager.data.model

/**
 * Represents an individual file asset available in a GitHub Release.
 */
data class ReleaseFileItem(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long = 0L,
    val sizeFormatted: String = ""
)
