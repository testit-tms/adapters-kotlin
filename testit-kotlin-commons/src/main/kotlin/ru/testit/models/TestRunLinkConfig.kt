package ru.testit.models

import kotlinx.serialization.Serializable

@Serializable
data class TestRunLinkConfig(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
)
