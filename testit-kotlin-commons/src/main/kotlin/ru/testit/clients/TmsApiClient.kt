package ru.testit.clients

import org.slf4j.LoggerFactory
import kotlinx.serialization.Contextual
import ru.testit.kotlin.client.apis.AttachmentsApi
import ru.testit.kotlin.client.apis.AutoTestsApi
import ru.testit.kotlin.client.apis.TestResultsApi
import ru.testit.kotlin.client.apis.TestRunsApi
import ru.testit.kotlin.client.infrastructure.ApiClient
import kotlinx.serialization.Serializable
import ru.testit.kotlin.client.models.*
import java.io.File
import java.time.Duration
import java.util.*


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
//        val apiClient = ApiClient(clientConfiguration.url)

        testRunsApi = TestRunsApi(clientConfiguration.url)
        init(testRunsApi)
        autoTestsApi = AutoTestsApi(clientConfiguration.url)
        init(autoTestsApi)
        attachmentsApi = AttachmentsApi(clientConfiguration.url)
        init(attachmentsApi)
        testResultsApi = TestResultsApi(clientConfiguration.url)
        init(testResultsApi)
    }

    fun init(client: ApiClient ) {
        client.apiKeyPrefix["Authorization"] = AUTH_PREFIX
        client.apiKey["Authorization"] = clientConfiguration.privateToken
        client.verifyingSsl = clientConfiguration.certValidation
    }

    override fun createTestRun(): TestRunV2GetModel {
        val model = TestRunV2PostShortModel(
            projectId = UUID.fromString(clientConfiguration.projectId)
        )

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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            LOGGER.error("Failed to send test results for test run $testRunUuid", e)
            throw e
        }
    }

    override fun addAttachment(path: String): String {
        val file = File(path)
        try {
            var model = attachmentsApi.apiV2AttachmentsPost(file);
            return model.id.toString();
        } catch (e: Exception) {
            LOGGER.error("Failed to upload attachment from path $path", e)
            throw e
        }
    }

    @Synchronized
    override fun linkAutoTestToWorkItems(id: String, workItemIds: Iterable<String>) {
        for (workItemId in workItemIds) {
            LOGGER.debug("Link autotest {} to workitem {}", id, workItemId)

            for (attempts in 0 until MAX_TRIES) {
                try {
                    autoTestsApi.linkAutoTestToWorkItem(id, WorkItemIdModel(workItemId))
                    LOGGER.debug("Link autotest {} to workitem {} is successfully", id, workItemId)
                    break
                } catch (e: Exception) {
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
            } catch (e: Exception) {
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
        println(model)
        return requireNotNull(autoTestsApi.createAutoTest(model).id.toString())
    }

    @Synchronized
    override fun getAutoTestByExternalId(externalId: String): AutoTestModel? {
        val projectIds = hashSetOf(UUID.fromString(clientConfiguration.projectId))
        val externalIds = hashSetOf(externalId)
        val filter = AutotestFilterModel(
            projectIds = projectIds,
            isDeleted = false,
            externalIds = externalIds
        )

        val includes = SearchAutoTestsQueryIncludesModel(INCLUDE_STEPS, INCLUDE_LINKS, INCLUDE_LABELS)

        val model = AutotestsSelectModel(filter, includes)


        val tests = autoTestsApi.apiV2AutoTestsSearchPost(null, null, null, null, null, model)

        if (tests.isEmpty()) {
            return null
        }

        return tests[0]
    }
}