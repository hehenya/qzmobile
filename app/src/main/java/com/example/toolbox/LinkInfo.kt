package com.example.toolbox.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkInfo(
    @SerialName("url") val url: String,
    @SerialName("title") val title: String,
    @SerialName("domain") val domain: String,
    @SerialName("fetched_at") val fetchedAt: String
)