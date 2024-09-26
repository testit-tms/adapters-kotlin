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
        privateToken_ = properties.getProperty(AppProperties.PRIVATE_TOKEN).toString(),
        projectId = properties.getProperty(AppProperties.PROJECT_ID).toString(),
        url = Utils.urlTrim(properties.getProperty(AppProperties.URL).toString()),
        configurationId = properties.getProperty(AppProperties.CONFIGURATION_ID).toString(),
        testRunId = properties.getProperty(AppProperties.TEST_RUN_ID).toString(),
        testRunName = properties.getProperty(AppProperties.TEST_RUN_NAME).toString(),
        certValidation = try {
            val validationCert = properties.getProperty(AppProperties.CERT_VALIDATION).toString()
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