package ru.testit.services

import ru.testit.clients.ClientConfiguration
import ru.testit.properties.AdapterConfig
import java.util.Properties

class ConfigManager(private val properties: Properties) {
    fun getAdapterConfig(): AdapterConfig {
        return AdapterConfig(properties)
    }

    fun getClientConfiguration(): ClientConfiguration {
        return ClientConfiguration(properties)
    }
}