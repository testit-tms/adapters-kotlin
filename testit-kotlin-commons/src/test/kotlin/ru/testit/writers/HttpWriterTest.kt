package ru.testit.writers

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import ru.testit.Helper
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.services.ResultStorage
import java.util.UUID
import kotlin.test.Test

class HttpWriterTest {
    companion object {
        const val TEST_RUN_ID = "5819479d-e38b-40d0-9e35-c5b2dab50158"
    }

    private lateinit var config: ClientConfiguration
    private lateinit var client: ApiClient
    private lateinit var storage: ResultStorage

    @BeforeEach
    fun init() {
        config = mockk()
        client = mockk()
        storage = mockk()

        every { config.url } returns "https://example.test/"
        every { config.projectId } returns "d7defd1e-c1ed-400d-8be8-091ebfdda744"
        every { config.configurationId } returns "b09d7164-d58c-41a5-9780-89c30e0cc0c7"
        every { config.privateToken } returns "QwertyT0kentPrivate"
        every { config.testRunId } returns TEST_RUN_ID
    }

    @Test
    fun writeTest_WithExistingAutoTest_InvokeUpdateHandler() {
        val testResult = Helper.generateTestResult()
        val response = Helper.generateAutoTestModel(config.projectId)
        val request = Helper.generateAutoTestPutModel(config.projectId)
        val uuids = Helper.generateListUuid()
        val strUuids = uuids.map { x -> x.toString() }

        // request.id = null

        every { client.getAutoTestByExternalId(testResult.externalId!!) } returns response
        every { client.sendTestResults(any(), any()) } returns strUuids
        justRun { client.updateAutoTest(any()) }
        every { client.getWorkItemsLinkedToTest(any()) } returns mutableListOf()
        justRun { client.linkAutoTestToWorkItems(any(), any()) }

        val writer = HttpWriter(config, client, storage)

        // act
        writer.writeTest(testResult)

        // assert
        verify(exactly = 1) {
            client.updateAutoTest(request)
        }
    }
}