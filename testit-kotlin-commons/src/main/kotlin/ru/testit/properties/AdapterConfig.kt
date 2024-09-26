package ru.testit.properties

import kotlinx.serialization.Serializable
import java.util.Properties

@Serializable
data class AdapterConfig(
    private var mode: AdapterMode = AdapterMode.USE_FILTER,
    var automaticCreationTestCases: Boolean = false,
    var tmsIntegration: Boolean = true
) {
    constructor(properties: Properties) : this() {
        try {
            val modeValue = properties.getProperty(AppProperties.ADAPTER_MODE).toString()
            this.mode = AdapterMode.valueOf(modeValue)
        } catch (e: Exception) {
            this.mode = AdapterMode.USE_FILTER
        }

        try {
            val automaticCreationTestCasesValue =
                properties.getProperty(AppProperties.AUTOMATIC_CREATION_TEST_CASES).toString()
            this.automaticCreationTestCases = automaticCreationTestCasesValue == "true"
        } catch (e: Exception) {
            this.automaticCreationTestCases = false
        }

        try {
            val tmsIntegrationValue =
                properties.getProperty(AppProperties.TMS_INTEGRATION).toString()
            this.tmsIntegration =
                tmsIntegrationValue != "false"
        } catch (e: Exception) {
            this.tmsIntegration = true
        }
    }

    fun getMode(): AdapterMode {
        return mode
    }

    fun shouldAutomaticCreationTestCases(): Boolean {
        return automaticCreationTestCases
    }

    fun shouldEnableTmsIntegration(): Boolean {
        return tmsIntegration
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("class AdapterConfig {\n")
        sb.append("    mode: ").append(this.mode).append("\n")
        sb.append("    automaticCreationTestCases: ").append(this.automaticCreationTestCases).append("\n")
        sb.append("    tmsIntegration: ").append(this.tmsIntegration).append("\n")
        sb.append("}")
        return sb.toString()
    }
}