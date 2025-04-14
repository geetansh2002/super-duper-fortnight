package com.example.test2

import kotlinx.serialization.Serializable

@Serializable
data class MediaProcessResponse(
    val vehicle_number: String,
    val is_number_found: Boolean,
    val vehicle_type: String,
    val is_carrying_contents: Boolean,
    val contents_details: ContentsDetails? = null,
    val confidence_scores: ConfidenceScores
)

@Serializable
data class ContentsDetails(
    val category: String?,
    val description: String?
)

@Serializable
data class ConfidenceScores(
    val vehicle_classification: Double,
    val cargo_detection: Double,
    val number_recognition: Double
)