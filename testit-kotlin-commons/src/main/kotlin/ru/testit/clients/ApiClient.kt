package ru.testit.clients

import ru.testit.kotlin.client.infrastructure.ApiException
import ru.testit.kotlin.client.models.*
import java.util.UUID

interface ApiClient {
    @Throws(ApiException::class)
    fun createTestRun(): TestRunV2GetModel

    @Throws(ApiException::class)
    fun getTestRun(uuid: String): TestRunV2GetModel

    @Throws(ApiException::class)
    fun completeTestRun(uuid: String)

    @Throws(ApiException::class)
    fun updateAutoTest(model: AutoTestPutModel)

    @Throws(ApiException::class)
    fun createAutoTest(model: AutoTestPostModel): String

    @Throws(ApiException::class)
    fun getAutoTestByExternalId(externalId: String): AutoTestModel?

    @Throws(ApiException::class)
    fun linkAutoTestToWorkItems(id: String, workItemIds: Iterable<String>)

    @Throws(ApiException::class)
    fun unlinkAutoTestToWorkItem(id: String, workItemId: String): Boolean

    @Throws(ApiException::class)
    fun getWorkItemsLinkedToTest(id: String): List<WorkItemIdentifierModel>

    @Throws(ApiException::class)
    fun sendTestResults(testRunUuid: String, models: List<AutoTestResultsForTestRunModel>): List<String>

    @Throws(ApiException::class)
    fun addAttachment(path: String): String

    @Throws(ApiException::class)
    fun getTestFromTestRun(testRunUuid: String, configurationId: String): List<String>

    @Throws(ApiException::class)
    fun getTestResult(uuid: UUID): TestResultModel

    @Throws(ApiException::class)
    fun updateTestResult(uuid: UUID, model: TestResultUpdateModel)
}
