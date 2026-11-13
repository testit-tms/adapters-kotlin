package ru.testit.properties

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdapterConfigTest {

    @Test
    fun constructor_WithoutImportRealtime_DefaultsToRealtime() {
        val config = AdapterConfig(Properties())

        assertTrue(config.shouldImportRealtime())
    }

    @Test
    fun constructor_ImportRealtimeFalse_DisablesRealtime() {
        val properties = Properties()
        properties.setProperty(AppProperties.IMPORT_REALTIME, "false")

        val config = AdapterConfig(properties)

        assertFalse(config.shouldImportRealtime())
    }

    @Test
    fun constructor_ImportRealtimeTrue_KeepsRealtime() {
        val properties = Properties()
        properties.setProperty(AppProperties.IMPORT_REALTIME, "true")

        val config = AdapterConfig(properties)

        assertTrue(config.shouldImportRealtime())
    }

    @Test
    fun constructor_ImportRealtimeInvalidValue_TreatedAsRealtime() {
        val properties = Properties()
        properties.setProperty(AppProperties.IMPORT_REALTIME, "maybe")

        val config = AdapterConfig(properties)

        assertTrue(config.shouldImportRealtime())
    }

    @Test
    fun constructor_WithoutSyncStoragePort_DefaultPort() {
        val config = AdapterConfig(Properties())

        assertEquals("49152", config.syncStoragePort)
    }

    @Test
    fun constructor_SyncStoragePortProvided_Applied() {
        val properties = Properties()
        properties.setProperty(AppProperties.SYNC_STORAGE_PORT, "51234")

        val config = AdapterConfig(properties)

        assertEquals("51234", config.syncStoragePort)
    }

    @Test
    fun constructor_SyncStoragePortBlank_KeepsDefault() {
        val properties = Properties()
        properties.setProperty(AppProperties.SYNC_STORAGE_PORT, "  ")

        val config = AdapterConfig(properties)

        assertEquals("49152", config.syncStoragePort)
    }

    @Test
    fun constructor_SyncStoragePortNullLiteral_KeepsDefault() {
        val properties = Properties()
        properties.setProperty(AppProperties.SYNC_STORAGE_PORT, "null")

        val config = AdapterConfig(properties)

        assertEquals("49152", config.syncStoragePort)
    }

    @Test
    fun toString_ContainsImportRealtimeAndSyncStoragePort() {
        val properties = Properties()
        properties.setProperty(AppProperties.IMPORT_REALTIME, "false")
        properties.setProperty(AppProperties.SYNC_STORAGE_PORT, "51234")

        val config = AdapterConfig(properties)

        val text = config.toString()
        assertTrue(text.contains("importRealtime: false"))
        assertTrue(text.contains("syncStoragePort: 51234"))
    }
}
