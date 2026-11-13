package ru.testit.services

import io.mockk.mockk
import io.mockk.verify
import ru.testit.Helper
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.models.ClassContainer
import ru.testit.models.MainContainer
import ru.testit.properties.AdapterConfig
import ru.testit.writers.Writer
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class AdapterManagerTest {

    private lateinit var writer: Writer

    @BeforeEach
    fun init() {
        writer = mockk(relaxed = true)
    }

    private fun newManager(importRealtime: Boolean): AdapterManager {
        val configuration = mockk<ClientConfiguration>(relaxed = true)
        val adapterConfig = AdapterConfig(tmsIntegration = true, importRealtime = importRealtime)
        val manager = AdapterManager(configuration, adapterConfig, mockk<ApiClient>(relaxed = true))

        val writerField = AdapterManager::class.java.getDeclaredField("writer")
        writerField.isAccessible = true
        writerField.set(manager, writer)

        return manager
    }

    @Test
    fun stopTestCase_ImportRealtimeFalse_SkipsWriteTest() {
        val manager = newManager(importRealtime = false)
        val uuid = UUID.randomUUID().toString()
        val testResult = Helper.generateTestResult().apply { this.uuid = uuid }

        manager.scheduleTestCase(testResult)
        manager.stopTestCase(uuid)

        verify(exactly = 0) { writer.writeTest(any()) }
    }

    @Test
    fun stopTestCase_ImportRealtimeTrue_WritesTest() {
        val manager = newManager(importRealtime = true)
        val uuid = UUID.randomUUID().toString()
        val testResult = Helper.generateTestResult().apply { this.uuid = uuid }

        manager.scheduleTestCase(testResult)
        manager.stopTestCase(uuid)

        verify(exactly = 1) { writer.writeTest(any()) }
    }

    @Test
    fun stopClassContainer_ImportRealtimeFalse_SkipsWriteClass() {
        val manager = newManager(importRealtime = false)
        val mainContainer = startContainers(manager)

        manager.stopClassContainer(mainContainer.second)

        verify(exactly = 0) { writer.writeClass(any()) }
    }

    @Test
    fun stopClassContainer_ImportRealtimeTrue_WritesClass() {
        val manager = newManager(importRealtime = true)
        val mainContainer = startContainers(manager)

        manager.stopClassContainer(mainContainer.second)

        verify(exactly = 1) { writer.writeClass(any()) }
    }

    @Test
    fun stopMainContainer_ImportRealtimeFalse_StillWritesTests() {
        val manager = newManager(importRealtime = false)
        val mainContainer = startContainers(manager)

        manager.stopMainContainer(mainContainer.first)

        verify(exactly = 1) { writer.writeTests(any()) }
    }

    @Test
    fun stopMainContainer_ImportRealtimeTrue_StillWritesTests() {
        val manager = newManager(importRealtime = true)
        val mainContainer = startContainers(manager)

        manager.stopMainContainer(mainContainer.first)

        verify(exactly = 1) { writer.writeTests(any()) }
    }

    private fun startContainers(manager: AdapterManager): Pair<String, String> {
        val mainUuid = UUID.randomUUID().toString()
        val classUuid = UUID.randomUUID().toString()

        manager.startMainContainer(MainContainer(uuid = mainUuid))
        manager.startClassContainer(mainUuid, ClassContainer(uuid = classUuid))

        return mainUuid to classUuid
    }
}
