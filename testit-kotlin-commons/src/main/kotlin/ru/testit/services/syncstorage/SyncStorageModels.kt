package ru.testit.services.syncstorage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val pid: String,
    @SerialName("testRunId")
    val testRunId: String
)

@Serializable
data class RegisterResponse(
    @SerialName("is_master")
    val isMaster: Boolean = false
)

@Serializable
data class SetWorkerStatusRequest(
    val pid: String,
    val status: String,
    @SerialName("testRunId")
    val testRunId: String
)

@Serializable
data class SetWorkerStatusResponse(
    val status: String = ""
)

@Serializable
data class TestResultCutApiModel(
    @SerialName("autoTestExternalId")
    val autoTestExternalId: String,
    @SerialName("statusCode")
    val statusCode: String,
    @SerialName("startedOn")
    val startedOn: String? = null
)

@Serializable
data class TestResultSaveResponse(
    val id: String = ""
)

@Serializable
data class HealthStatusResponse(
    val status: String = ""
)
