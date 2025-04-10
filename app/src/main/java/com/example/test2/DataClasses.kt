package com.example.test2

import kotlinx.serialization.Serializable

@Serializable
data class MediaProcessRequest(
    val bucket_urls: List<String>,
    val is_video: Boolean
)