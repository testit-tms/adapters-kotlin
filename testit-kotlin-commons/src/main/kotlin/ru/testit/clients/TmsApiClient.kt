package ru.testit.clients

import org.slf4j.LoggerFactory
import kotlinx.serialization.Contextual
import ru.testit.kotlin.client.api.AttachmentsApi
import ru.testit.kotlin.client.api.AutoTestsApi
import ru.testit.kotlin.client.api.TestResultsApi
import ru.testit.kotlin.client.api.TestRunsApi
import ru.testit.kotlin.client.infrastructure.ApiException
import ru.testit.kotlin.client.infrastructure.ApiClient
import kotlinx.serialization.Serializable
import ru.testit.kotlin.client.models.*
import java.io.File
import java.time.Duration
import java.util.*

@Serializable
class TmsApiClient(private val clientConfiguration: ClientConfiguration) : ru.testit.clients.ApiClient {

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(javaClass)
        private const val AUTH_PREFIX = "PrivateToken"
        private const val INCLUDE_STEPS = true
        private const val INCLUDE_LABELS = true
        private const val INCLUDE_LINKS = true
        private const val MAX_TRIES = 10
        private const val WAITING_TIME = 100
    }

    @Contextual
    private val testRunsApi: TestRunsApi
    @Contextual
    private val autoTestsApi: AutoTestsApi
    @Contextual
    private val attachmentsApi: AttachmentsApi
    @Contextual
    private val testResultsApi: TestResultsApi

    init {
        val apiClient = ApiClient(clientConfiguration.url)
        apiClient.apiKeyPrefix["Authorization"] = AUTH_PREFIX
        apiClient.apiKey["Authorization"] = clientConfiguration.privateToken
        apiClient.verifyingSsl = clientConfiguration.certValidation

//            .apply {
//            setBasePath(clientConfiguration.url)
//            setApiKeyPrefix(AUTH_PREFIX)
//            setApiKey(clientConfiguration.privateToken)
//            setVerifyingSsl(clientConfiguration.certValidation)
//        }

        testRunsApi = TestRunsApi()
        autoTestsApi = AutoTestsApi()
        attachmentsApi = AttachmentsApi()
        testResultsApi = TestResultsApi()
    }

    override fun createTestRun(): TestRunV2GetModel {
        val model = TestRunV2PostShortModel().apply {
            projectId = UUID.fromString(clientConfiguration.projectId)
        }

        LOGGER.debug("Create new test run: {}", model);

        var response = testRunsApi.createEmpty(model)
        testRunsApi.startTestRun(response.id).also { run ->
            LOGGER.debug("The test run created: {}", response)
        }

        return response
    }

    override fun getWorkItemsLinkedToTest(testId: String): List<WorkItemIdentifierModel> {
        try {
            return autoTestsApi.getWorkItemsLinkedToAutoTest(testId, false, false)
        } catch (e: ApiException) {
            LOGGER.error("Failed to retrieve work items linked to test $testId", e)
            throw e
        }
    }

    override fun sendTestResults(
        testRunUuid: String,
        models: List<AutoTestResultsForTestRunModel>
    ): List<String> {
        try {
            return testRunsApi.setAutoTestResultsForTestRun(UUID.fromString(testRunUuid), models).map { it.toString() }
        } catch (e: ApiException) {
            LOGGER.error("Failed to send test results for test run $testRunUuid", e)
            throw e
        }
    }

    override fun addAttachment(path: String): String {
        val file = File(path)
        try {
            var model = attachmentsApi.apiV2AttachmentsPost(file);
            return model.id.toString();
        } catch (e: ApiException) {
            LOGGER.error("Failed to upload attachment from path $path", e)
            throw e
        }
    }

    // TODO: threads?
    @Synchronized
    override fun linkAutoTestToWorkItems(id: String, workItemIds: Iterable<String>) {
        for (workItemId in workItemIds) {
            LOGGER.debug("Link autotest {} to workitem {}", id, workItemId)

            for (attempts in 0 until MAX_TRIES) {
                try {
                    autoTestsApi.linkAutoTestToWorkItem(id, WorkItemIdModel(workItemId))
                    LOGGER.debug("Link autotest {} to workitem {} is successfully", id, workItemId)
                    break
                } catch (e: ApiException) {
                    LOGGER.error("Cannot link autotest {} to work item {}", id, workItemId)

                    try {
                        Thread.sleep(Duration.ofMillis(100).toMillis())
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                }
            }
        }
    }
    override fun getTestFromTestRun(testRunUuid: String, configurationId: String): List<String> {
        val model = testRunsApi.getTestRunById(UUID.fromString(testRunUuid))
        val configUUID = UUID.fromString(configurationId)

        return if (model.testResults.isNullOrEmpty()) emptyList() else
            model.testResults!!.filter { it.configurationId == configUUID }
                .mapNotNull { it.autoTest?.externalId }.toList()
    }

    override fun getTestResult(uuid: UUID): TestResultModel {
        return testResultsApi.apiV2TestResultsIdGet(uuid)
    }

    override fun updateTestResult(uuid: UUID, model: TestResultUpdateModel) {
        testResultsApi.apiV2TestResultsIdPut(uuid, model)
    }

    override fun unlinkAutoTestToWorkItem(testId: String, workItemId: String): Boolean {
        for (i in 1..MAX_TRIES) {
            try {
                autoTestsApi.deleteAutoTestLinkFromWorkItem(testId, workItemId)
                LOGGER.debug("Unlinked autotest $testId from workitem $workItemId")
                return true
            } catch (e: ApiException) {
                LOGGER.error("Failed to unlink autotest $testId from work item $workItemId", e)
                if (i == MAX_TRIES) throw e
                try {
                    Thread.sleep(Duration.ofMillis(100).toMillis())
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
        return false
    }

//    suspend fun updateTestResult(uuid: UUID, model: TestResultUpdateModel): Unit = runCatching {
//        testResultsApi.updateTestResult(uuid, model)
//    }.onFailure { e ->
//        LOGGER.error("Failed to update test result with UUID $uuid", e)
//    }

//    TODO: TBD

    @Synchronized
    override fun getTestRun(uuid: String): TestRunV2GetModel {
        return testRunsApi.getTestRunById(UUID.fromString(uuid))
    }

    @Synchronized
    override fun completeTestRun(uuid: String) {
        testRunsApi.completeTestRun(UUID.fromString(uuid))
    }

    @Synchronized
    override fun updateAutoTest(model: AutoTestPutModel) {
        autoTestsApi.updateAutoTest(model)
    }

    @Synchronized
    override fun createAutoTest(model: AutoTestPostModel): String {
        return requireNotNull(autoTestsApi.createAutoTest(model).id.toString())
    }

    @Synchronized
    override fun getAutoTestByExternalId(externalId: String): AutoTestModel? {
        val filter = AutotestFilterModel()

        val projectIds = hashSetOf(UUID.fromString(clientConfiguration.projectId))
        filter.projectIds = projectIds
        filter.isDeleted = false

        val externalIds = hashSetOf(externalId)
        filter.externalIds = externalIds

        val includes = SearchAutoTestsQueryIncludesModel(INCLUDE_STEPS, INCLUDE_LINKS, INCLUDE_LABELS)

        val model = AutotestsSelectModel(filter, includes)


        val tests = autoTestsApi.apiV2AutoTestsSearchPost(null, null, null, null, null, model)

        if (tests.isEmpty()) {
            return null
        }

        return tests[0]
    }
}