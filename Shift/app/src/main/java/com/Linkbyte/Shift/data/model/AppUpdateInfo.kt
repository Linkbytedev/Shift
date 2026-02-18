package com.Linkbyte.Shift.data.model

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String? = null
)
