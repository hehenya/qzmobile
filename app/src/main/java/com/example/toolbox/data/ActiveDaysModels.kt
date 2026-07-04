package com.example.toolbox.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveDaysResponse(
    val success: Boolean,
    @SerialName("active_days") val activeDays: List<ActiveDay> = emptyList(),
    val pagination: PaginationInfo? = null
)

@Serializable
data class ActiveDay(
    val date: String,
    @SerialName("msg_count") val msgCount: Int
)

@Serializable
data class PaginationInfo(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val pages: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_prev") val hasPrev: Boolean
)