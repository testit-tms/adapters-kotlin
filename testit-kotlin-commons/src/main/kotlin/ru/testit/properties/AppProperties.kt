package ru.testit.properties

import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*


object AppProperties {

val URL = "url"
val PRIVATE_TOKEN = "privateToken"
val PROJECT_ID = "projectId"
val CONFIGURATION_ID = "configurationId"
val TEST_RUN_ID = "testRunId"
val TEST_RUN_NAME = "testRunName"
val ADAPTER_MODE = "adapterMode"
val AUTOMATIC_CREATION_TEST_CASES = "automaticCreationTestCases"
val AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES = "automaticUpdationLinksToTestCases"
val CERT_VALIDATION = "certValidation"
val TMS_INTEGRATION = "testIt"

val PROPERTIES_FILE = "testit.properties"

private val log = LoggerFactory.getLogger(javaClass)

private val envVarsNames: Map<String, Map<String, String>> = mapOf(
    "env" to mapOf(
        URL to "TMS_URL",
        PRIVATE_TOKEN to "TMS_PRIVATE_TOKEN",
        PROJECT_ID to "TMS_PROJECT_ID",
        CONFIGURATION_ID to "TMS_CONFIGURATION_ID",
        TEST_RUN_ID to "TMS_TEST_RUN_ID",
        TEST_RUN_NAME to "TMS_TEST_RUN_NAME",
        ADAPTER_MODE to "TMS_ADAPTER_MODE",
        AUTOMATIC_CREATION_TEST_CASES to "TMS_AUTOMATIC_CREATION_TEST_CASES",
        CERT_VALIDATION to "TMS_CERT_VALIDATION",
        TMS_INTEGRATION to "TMS_TEST_IT"
    ),
    "cli" to mapOf(
        URL to "tmsUrl",
        PRIVATE_TOKEN to "tmsPrivateToken",
        PROJECT_ID to "tmsProjectId",
        CONFIGURATION_ID to "tmsConfigurationId",
        TEST_RUN_ID to "tmsTestRunId",
        TEST_RUN_NAME to "tmsTestRunName",
        ADAPTER_MODE to "tmsAdapterMode",
        AUTOMATIC_CREATION_TEST_CASES to "tmsAutomaticCreationTestCases",
        CERT_VALIDATION to "tmsCertValidation",
        TMS_INTEGRATION to "tmsTestIt"
    )
)

    fun loadProperties(): Properties {
        val configFile = getConfigFileName()
        val properties = Properties()

        loadPropertiesFrom(Thread.currentThread().contextClassLoader, properties, configFile)
        loadPropertiesFrom(ClassLoader.getSystemClassLoader(), properties, configFile)

        val token = properties.getProperty(PRIVATE_TOKEN)
        if (token?.isNotEmpty() == true && !token.equals("null", ignoreCase = true)) {
            log.warn("The configuration file specifies a private token. It is not safe. Use TMS_PRIVATE_TOKEN environment variable")
        }

        val systemProps = System.getProperties()
        val envProps = Properties().apply { putAll(System.getenv()) }

        properties.putAll(loadPropertiesFromEnv(systemProps, envVarsNames.getOrDefault("env", emptyMap())))
        properties.putAll(loadPropertiesFromEnv(envProps, envVarsNames.getOrDefault("env", emptyMap())))
        properties.putAll(loadPropertiesFromEnv(systemProps, envVarsNames.getOrDefault("cli", emptyMap())))
        properties.putAll(loadPropertiesFromEnv(envProps, envVarsNames.getOrDefault("cli", emptyMap())))

        return if (properties.getProperty(TMS_INTEGRATION, "true").equals("false")) {
            return properties
        } else {
            return validateProperties(properties)
        }
    }

    private fun loadPropertiesFrom(classLoader: ClassLoader?, properties: Properties, fileName: String) {
        classLoader?.getResourceAsStream(fileName)?.use { stream ->
            val newProps = Properties().apply { load(stream) }

            for ((key, value) in newProps) {
                if (value.toString() != "") {
                    properties.setProperty(key.toString(), value.toString())
                }
            }
        } ?: log.error("Exception while reading properties: {}", fileName)
    }


    private fun loadPropertiesFromEnv(properties: Properties, varNames: Map<String, String>): Map<String, String>
    {
        val result = mutableMapOf<String, String>()

        try {
            val url = properties.getProperty(varNames[URL]) ?: return result
            URI(url) // Try to create a URI to validate the URL
            result[URL] = url
        } catch (e: Exception) {
            // Ignore exceptions related to invalid URLs
        }

        val token = properties.getProperty(varNames[PRIVATE_TOKEN])
        if (!token.isNullOrBlank() && !token.equals("null")) {
            result[PRIVATE_TOKEN] = token
        }

        val projectId = properties.getProperty(varNames[PROJECT_ID])
        if (!projectId.isNullOrBlank()) {
            try {
                UUID.fromString(projectId)
                result[PROJECT_ID] = projectId
            } catch (_: Exception) {}
        }

        val configurationId = properties.getProperty(varNames[CONFIGURATION_ID])
        if (!configurationId.isNullOrBlank()) {
            try {
                UUID.fromString(configurationId)
                result[CONFIGURATION_ID] = configurationId
            } catch (_: Exception) {}
        }

        val testRunId = properties.getProperty(varNames[TEST_RUN_ID])
        if (!testRunId.isNullOrBlank()) {
            try {
                UUID.fromString(testRunId)
                result[TEST_RUN_ID] = testRunId
            } catch (_: Exception) {}
        }

        val testRunName = properties.getProperty(varNames[TEST_RUN_NAME])
        if (!testRunName.isNullOrBlank() && !testRunName.equals("null")) {
            result[TEST_RUN_NAME] = testRunName
        }


        val adapterMode = properties.getProperty(varNames[ADAPTER_MODE])?.toIntOrNull()
        if (adapterMode != null && 0 <= adapterMode && adapterMode <= 2) {
            result[ADAPTER_MODE] = adapterMode.toString()
        }


        val createTestCases = properties.getProperty(varNames[AUTOMATIC_CREATION_TEST_CASES])
        if (createTestCases == "false" || createTestCases == "true") {
            result[AUTOMATIC_CREATION_TEST_CASES] = createTestCases
        }


        val updateLinksToTestCases = properties.getProperty(varNames[AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES])
        if (updateLinksToTestCases == "false" || updateLinksToTestCases == "true") {
            result[AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES] = updateLinksToTestCases
        }

        val certValidation = properties.getProperty(varNames[CERT_VALIDATION])
        if (certValidation == "false" || certValidation == "true") {
            result[CERT_VALIDATION] = certValidation
        }

        val tmsIntegration = properties.getProperty(varNames[TMS_INTEGRATION])
        if (tmsIntegration == "false" || tmsIntegration == "true") {
            result[TMS_INTEGRATION] = tmsIntegration
        }

        return result
    }

    fun validateProperties(properties: Properties): Properties {
        val errorsBuilder = StringBuilder()

        val token = properties.getProperty(PRIVATE_TOKEN)
        if (token.isNullOrBlank() || token.equals("null", ignoreCase = true)) {
            log.error("Invalid token: $token")
            errorsBuilder.appendLine("Invalid token: $token")
        }

        try {
            UUID.fromString(properties.getProperty(PROJECT_ID))
        } catch (e: Exception) {
            log.error("Invalid projectId: ${e.message}")
            errorsBuilder.appendLine("Invalid projectId: ${e.message}")
        }

        try {
            UUID.fromString(properties.getProperty(CONFIGURATION_ID))
        } catch (e: Exception) {
            log.error("Invalid configurationId: ${e.message}")
            errorsBuilder.appendLine("Invalid configurationId: ${e.message}")
        }

        val adapterModeStr = properties.getProperty(ADAPTER_MODE)
        var adapterMode = 0
        try {
            adapterMode = adapterModeStr?.toInt() ?: 0
        } catch (e: Exception) {
            log.warn("Invalid adapterMode: ${adapterModeStr}. Use default value instead: 0", e)
            properties.setProperty(ADAPTER_MODE, "0")
        }

        val testRunId = properties.getProperty(TEST_RUN_ID)
        if (testRunId != null) {
            try {
                UUID.fromString(testRunId)
                if (adapterMode == 2) {
                    log.error("Adapter works in mode 2. Config should not contains test run id.")
                    errorsBuilder.appendLine("Adapter works in mode 2. Config should not contains test run id.")
                }
            } catch (e: Exception) {
                if (adapterMode == 0 || adapterMode == 1) {
                    log.error("Invalid testRunId: ${e.message}")
                    errorsBuilder.appendLine("Invalid testRunId: ${e.message}")
                }
            }
        }

        val createTestCases = properties.getProperty(AUTOMATIC_CREATION_TEST_CASES)
        if (createTestCases != "false" && createTestCases != "true") {
            log.warn("Invalid autoCreateTestCases: $createTestCases. Use default value instead: false")
            properties.setProperty(AUTOMATIC_CREATION_TEST_CASES, "false")
        }

        val updateLinksToTestCases = properties.getProperty(AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES)
        if (updateLinksToTestCases != "false" && updateLinksToTestCases != "true") {
            log.warn("Invalid autoUpdateLinksToTestCases: $updateLinksToTestCases. Use default value instead: false")
            properties.setProperty(AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES, "false")
        }

        val certValidation = properties.getProperty(CERT_VALIDATION)
        if (certValidation != "false" && certValidation != "true") {
            log.warn("Invalid certValidation: $certValidation. Use default value instead: true")
            properties.setProperty(CERT_VALIDATION, "true")
        }

        val tmsIntegration = properties.getProperty(TMS_INTEGRATION)
        if (tmsIntegration != "false" && tmsIntegration != "true") {
            log.warn("Invalid tmsIntegration: $tmsIntegration. Use default value instead: true")
            properties.setProperty(TMS_INTEGRATION, "true")
        }

        val errors = errorsBuilder.toString()
        if (!errors.isEmpty()) {
            throw AssertionError("Invalid configuration provided : $errors")
        }

        return properties
    }

    fun getConfigFileName(): String {
        return try {
            System.getProperty("TMS_CONFIG_FILE")?.takeIf { it.isNotBlank() && it != "null" } ?: PROPERTIES_FILE
        } catch (e: SecurityException) {
            PROPERTIES_FILE
        }
    }


}