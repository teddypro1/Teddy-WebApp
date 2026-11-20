package com.teddy.bpositive.model

data class UpdateMetadata(
    val enabled: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Long,
    val minimumSupportedVersionCode: Long,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String
)
