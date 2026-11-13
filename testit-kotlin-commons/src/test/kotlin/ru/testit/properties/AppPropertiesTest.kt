package ru.testit.properties

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppPropertiesTest {

    private val systemPropertyKeys = listOf(
        "tmsUrl",
        "tmsPrivateToken",
        "tmsProjectId",
        "tmsConfigurationId",
        "tmsImportRealtime",
        "tmsSyncStoragePort"
    )

    @AfterTest
    fun cleanup() {
        systemPropertyKeys.forEach { System.clearProperty(it) }
    }

    @Test
    fun loadProperties_ImportRealtimeFalseSystemProperty_Applied() {
        setRequiredProperties()
        System.setProperty("tmsImportRealtime", "false")

        val properties = AppProperties.loadProperties()

        assertEquals("false", properties.getProperty(AppProperties.IMPORT_REALTIME))
    }

    @Test
    fun loadProperties_ImportRealtimeTrueSystemProperty_Applied() {
        setRequiredProperties()
        System.setProperty("tmsImportRealtime", "true")

        val properties = AppProperties.loadProperties()

        assertEquals("true", properties.getProperty(AppProperties.IMPORT_REALTIME))
    }

    @Test
    fun loadProperties_ImportRealtimeInvalidValue_Dropped() {
        setRequiredProperties()
        System.setProperty("tmsImportRealtime", "maybe")

        val properties = AppProperties.loadProperties()

        assertNull(properties.getProperty(AppProperties.IMPORT_REALTIME))
    }

    @Test
    fun loadProperties_WithoutImportRealtime_Absent() {
        setRequiredProperties()

        assertNull(AppProperties.loadProperties().getProperty(AppProperties.IMPORT_REALTIME))
    }

    @Test
    fun loadProperties_SyncStoragePortSystemProperty_Applied() {
        setRequiredProperties()
        System.setProperty("tmsSyncStoragePort", "51234")

        val properties = AppProperties.loadProperties()

        assertEquals("51234", properties.getProperty(AppProperties.SYNC_STORAGE_PORT))
    }

    private fun setRequiredProperties() {
        System.setProperty("tmsUrl", "https://example.test/")
        System.setProperty("tmsPrivateToken", "QwertyT0kentPrivate")
        System.setProperty("tmsProjectId", "d7defd1e-c1ed-400d-8be8-091ebfdda744")
        System.setProperty("tmsConfigurationId", "b09d7164-d58c-41a5-9780-89c30e0cc0c7")
    }
}
