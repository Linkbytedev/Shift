package com.Linkbyte.Shift.data.model

data class VaultImage(
    val id: String = "",
    val fileName: String = "",
    val addedAt: Long = 0,
    val encryptedPath: String = "", // Path to encrypted full image
    val thumbnailPath: String = "" // Path to encrypted thumbnail
)
