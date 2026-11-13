package ru.testit.writers

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import ru.testit.Helper
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.kotlin.adaptersapi.models.AutoTestResultsForTestRunModel
import ru.testit.kotlin.adaptersapi.models.TestResultResponse
import ru.testit.kotlin.adaptersapi.models.TestStatusApiResult
import ru.testit.kotlin.adaptersapi.models.TestStatusApiType
import ru.testit.kotlin.adaptersapi.models.TestStatusType
import ru.testit.models.ClassContainer
import ru.testit.models.MainContainer
import ru.testit.services.ResultStorage
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val response = Helper.generateAutoTestApiResult(config.projectId)
        val request = Helper.generateAutoTestPutModel(config.projectId)
        val uuids = Helper.generateListUuid()
        val strUuids = uuids.map { x -> x.toString() }

        // request.id = null

        every { client.getAutoTestByExternalId(testResult.externalId!!) } returns response
        every { client.getTestResultIdByExternalId(any(), any(), any()) } returns null
        every { client.sendTestResults(any(), any()) } returns strUuids
        justRun { client.updateAutoTest(any()) }
        every { client.getWorkItemsLinkedToTest(any()) } returns mutableListOf()
        justRun { client.linkAutoTestToWorkItems(any(), any()) }

        val writer = HttpWriter(config, client, storage)

        // act
        writer.writeTest(testResult)

        // TODO: fix test
        // assert
        // verify(exactly = 1) {
        //     client.updateAutoTest(request)
        // }
    }

    @Test
    fun writeTests_NoExistingTestResult_SendsFinalResultInBatch() {
        val suite = deferredSuite()
        stubAutoTest()
        every { client.getTestResultIdByExternalId(any(), any(), any()) } returns null
        every { client.sendTestResults(any(), any()) } returns listOf(UUID.randomUUID().toString())

        HttpWriter(config, client, suite.storage).writeTests(suite.mainContainer)

        val results = slot<List<AutoTestResultsForTestRunModel>>()
        verify(exactly = 1) { client.sendTestResults(TEST_RUN_ID, capture(results)) }
        verify(exactly = 0) { client.updateTestResult(any(), any()) }
        assertEquals(1, results.captured.size)
        assertEquals(suite.testResult.externalId, results.captured[0].autoTestExternalId)
        assertEquals(TestStatusType.Succeeded, results.captured[0].statusType)
    }

    @Test
    fun writeTests_ExistingResultInProgress_SendsFinalResultInsteadOfUpdate() {
        val suite = deferredSuite()
        stubAutoTest()
        val existingResultId = UUID.randomUUID()
        every { client.getTestResultIdByExternalId(any(), any(), any()) } returns existingResultId
        every { client.getTestResult(existingResultId) } returns testResultResponse("InProgress")
        every { client.sendTestResults(any(), any()) } returns listOf(UUID.randomUUID().toString())

        HttpWriter(config, client, suite.storage).writeTests(suite.mainContainer)

        val results = slot<List<AutoTestResultsForTestRunModel>>()
        verify(exactly = 1) { client.sendTestResults(TEST_RUN_ID, capture(results)) }
        verify(exactly = 0) { client.updateTestResult(any(), any()) }
        assertEquals(TestStatusType.Succeeded, results.captured[0].statusType)
    }

    @Test
    fun writeTests_ExistingResultWithFinalStatus_UpdatesTestResult() {
        val suite = deferredSuite()
        stubAutoTest()
        val existingResultId = UUID.randomUUID()
        every { client.getTestResultIdByExternalId(any(), any(), any()) } returns existingResultId
        every { client.getTestResult(existingResultId) } returns testResultResponse("Passed")
        justRun { client.updateTestResult(any(), any()) }

        HttpWriter(config, client, suite.storage).writeTests(suite.mainContainer)

        verify(exactly = 1) { client.updateTestResult(existingResultId, any()) }
        verify(exactly = 0) { client.sendTestResults(any(), any()) }
    }

    private class DeferredSuite(
        val storage: ResultStorage,
        val mainContainer: MainContainer,
        val testResult: ru.testit.models.TestResultCommon
    )

    private fun deferredSuite(): DeferredSuite {
        val storage = ResultStorage()
        val testResult = Helper.generateTestResult().apply { uuid = UUID.randomUUID().toString() }
        val classUuid = UUID.randomUUID().toString()
        val mainUuid = UUID.randomUUID().toString()
        val classContainer = ClassContainer(uuid = classUuid).apply { children.add(testResult.uuid!!) }
        val mainContainer = MainContainer(uuid = mainUuid).apply { children.add(classUuid) }

        storage.put(mainUuid, mainContainer)
        storage.put(classUuid, classContainer)
        storage.put(testResult.uuid!!, testResult)

        return DeferredSuite(storage, mainContainer, testResult)
    }

    private fun stubAutoTest() {
        every { client.getAutoTestByExternalId(Helper.EXTERNAL_ID) } returns Helper.generateAutoTestApiResult(config.projectId)
        justRun { client.updateAutoTest(any()) }
        every { client.getWorkItemsLinkedToTest(any()) } returns mutableListOf()
        justRun { client.linkAutoTestToWorkItems(any(), any()) }
    }

    private fun testResultResponse(statusCode: String): TestResultResponse {
        return TestResultResponse(
            id = UUID.randomUUID(),
            failureClassIds = emptyList(),
            configurationId = UUID.fromString(config.configurationId),
            testPointId = UUID.randomUUID(),
            testRunId = UUID.fromString(TEST_RUN_ID),
            status = TestStatusApiResult(
                id = UUID.randomUUID(),
                type = TestStatusApiType.Succeeded,
                code = statusCode
            )
        )
    }
}