package ru.testit.clients

import ru.testit.kotlin.client.infrastructure.ClientException
import ru.testit.kotlin.client.infrastructure.ServerException
import ru.testit.kotlin.client.models.*
import java.io.IOException
import java.util.UUID

interface ApiClient {
    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun createTestRun(): TestRunV2ApiResult

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun updateTestRun()

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getTestRun(uuid: String): TestRunV2ApiResult

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun completeTestRun(uuid: String)

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun updateAutoTest(model: AutoTestUpdateApiModel)

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun createAutoTest(model: AutoTestCreateApiModel): String

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getAutoTestByExternalId(externalId: String): AutoTestApiResult?

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun linkAutoTestToWorkItems(id: String, workItemIds: Iterable<String>)

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun unlinkAutoTestToWorkItem(id: String, workItemId: String): Boolean

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getWorkItemsLinkedToTest(id: String): List<AutoTestWorkItemIdentifierApiResult>

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun sendTestResults(testRunUuid: String, models: List<AutoTestResultsForTestRunModel>): List<String>

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun addAttachment(path: String): String

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getTestFromTestRun(testRunUuid: String, configurationId: String): List<String>

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun getTestResult(uuid: UUID): TestResultResponse

    @Throws(IllegalStateException::class, IOException::class,
        UnsupportedOperationException::class, ClientException::class, ServerException::class)
    fun updateTestResult(uuid: UUID, model: TestResultUpdateV2Request )
}
