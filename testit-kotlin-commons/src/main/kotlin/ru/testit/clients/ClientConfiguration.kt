package ru.testit.clients

import kotlinx.serialization.Serializable
import ru.testit.properties.AppProperties
import ru.testit.services.Utils

import java.util.*

@Serializable
data class ClientConfiguration(
    private val privateToken_: String,
    val projectId: String,
    val url: String,
    val configurationId: String,
    var testRunId: String,
    val testRunName: String,
    val certValidation: Boolean,
    var automaticUpdationLinksToTestCases: Boolean
) {
    constructor(properties: Properties) : this(
        privateToken_ = properties.getProperty(AppProperties.PRIVATE_TOKEN, "null"),
        projectId = properties.getProperty(AppProperties.PROJECT_ID, "null"),
        url = Utils.urlTrim(properties.getProperty(AppProperties.URL, "null")),
        configurationId = properties.getProperty(AppProperties.CONFIGURATION_ID, "null"),
        testRunId = properties.getProperty(AppProperties.TEST_RUN_ID, "null"),
        testRunName = properties.getProperty(AppProperties.TEST_RUN_NAME, "null"),
        certValidation = try {
            val validationCert = properties.getProperty(AppProperties.CERT_VALIDATION, "null")
            if (validationCert == "null") {
                "true"
            } else {
                validationCert
            }
        } catch (e: NullPointerException) {
            "true"
        }.toBoolean(),
        automaticUpdationLinksToTestCases = try {
            properties.getProperty(AppProperties.AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES).toString() == "true"
        } catch (e: NullPointerException) {
            false
        }
    )

    val privateToken: String
        get() = privateToken_



    override fun toString(): String {
        return "ClientConfiguration(" +
                "url='$url', " +
                "privateToken='**********', " +
                "projectId='$projectId', " +
                "configurationId='$configurationId', " +
                "testRunId='$testRunId', " +
                "testRunName='$testRunName', " +
                "certValidation=$certValidation, " +
                "automaticUpdationLinksToTestCases=$automaticUpdationLinksToTestCases" +
                ")"
    }
}