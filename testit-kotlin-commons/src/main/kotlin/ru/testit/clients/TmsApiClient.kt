package ru.testit.clients

import kotlinx.serialization.Contextual
import org.slf4j.LoggerFactory
import ru.testit.kotlin.adaptersapi.apis.AttachmentsApi
import ru.testit.kotlin.adaptersapi.apis.AutoTestsApi
import ru.testit.kotlin.adaptersapi.apis.TestResultsApi
import ru.testit.kotlin.adaptersapi.apis.TestRunsApi
import ru.testit.kotlin.adaptersapi.infrastructure.ApiClient
import ru.testit.kotlin.adaptersapi.models.*
import ru.testit.properties.TestRunMetadataParser
import ru.testit.utils.HtmlEscapeUtils
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
        testRunsApi = TestRunsApi(clientConfiguration.url)
        init(testRunsApi)
        autoTestsApi = AutoTestsApi(clientConfiguration.url)
        init(autoTestsApi)
        attachmentsApi = AttachmentsApi(clientConfiguration.url)
        init(attachmentsApi)
        testResultsApi = TestResultsApi(clientConfiguration.url)
        init(testResultsApi)
    }

    fun init(client: ApiClient) {
        client.apiKeyPrefix["Authorization"] = AUTH_PREFIX
        client.apiKey["Authorization"] = clientConfiguration.privateToken
        client.verifyingSsl = clientConfiguration.certValidation
    }

    override fun createTestRun(): TestRunApiResult {
        val tags = clientConfiguration.testRunTags.takeIf { it.isNotEmpty() }
        val links = TestRunMetadataParser.toCreateLinkModels(clientConfiguration.testRunLinks)
            .takeIf { it.isNotEmpty() }

        val model = CreateEmptyTestRunApiModel(
            projectId = UUID.fromString(clientConfiguration.projectId),
            name = if (clientConfiguration.testRunName != "null") clientConfiguration.testRunName else null,
            tags = tags,
            links = links,
        )

        LOGGER.debug("Create new test run: {}", model)
        if (tags != null || links != null) {
            LOGGER.info(
                "Applying test run metadata on create: tags={}, links={}",
                tags ?: emptyList<String>(),
                links?.map { it.url } ?: emptyList<String>(),
            )
        }

        val response = testRunsApi.adaptersTestRunsPost(model)
        testRunsApi.adaptersTestRunsIdStartPost(response.id).also {
            LOGGER.debug("The test run created: {}", response)
        }

        return response
    }

    override fun updateTestRun() {
        LOGGER.debug("Update test run: {}", clientConfiguration.testRunId)

        val hasName = clientConfiguration.testRunName != "null"
        val hasMetadata = clientConfiguration.hasTestRunMetadata()
        if (!hasName && !hasMetadata) {
            return
        }

        val testRun = this.getTestRun(clientConfiguration.testRunId)
        val nameChanged = hasName && testRun.name != clientConfiguration.testRunName
        if (!nameChanged && !hasMetadata) {
            return
        }

        val existingLinks = testRun.links.map { link ->
            UpdateLinkApiModel(
                id = link.id,
                url = link.url,
                title = link.title,
                description = link.description,
                type = link.type,
            )
        }
        val configuredLinks = TestRunMetadataParser.toUpdateLinkModels(clientConfiguration.testRunLinks)
        val mergedTags = TestRunMetadataParser.mergeTags(testRun.tags, clientConfiguration.testRunTags)
        val mergedLinks = TestRunMetadataParser.mergeUpdateLinks(existingLinks, configuredLinks)

        val model = UpdateEmptyTestRunApiModel(
            id = testRun.id,
            name = if (nameChanged) clientConfiguration.testRunName else testRun.name,
            description = null,
            launchSource = null,
            attachments = testRun.attachments.map { AssignAttachmentApiModel(id = it.id) },
            links = mergedLinks,
            tags = mergedTags,
        )

        testRunsApi.adaptersTestRunsPut(model)

        if (hasMetadata) {
            LOGGER.info(
                "Merged test run metadata for {}: tags={}, links={}",
                clientConfiguration.testRunId,
                mergedTags,
                mergedLinks.map { it.url },
            )
        }
        LOGGER.debug("The test run updated")
    }

    override fun getWorkItemsLinkedToTest(testId: String): List<AutoTestWorkItemIdentifierApiResult> {
        try {
            return autoTestsApi.adaptersAutoTestsIdWorkItemsGet(testId, false, false)
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
            val escapedModels = models.map { model ->
                HtmlEscapeUtils.escapeHtmlInObject(model) ?: model
            }
            return testRunsApi.adaptersTestRunsIdTestResultsPost(
                UUID.fromString(testRunUuid),
                escapedModels
            ).map { it.toString() }
        } catch (e: Exception) {
            LOGGER.error("Failed to send test results for test run $testRunUuid", e)
            throw e
        }
    }

    override fun addAttachment(path: String): String {
        val file = File(path)
        try {
            val model = attachmentsApi.adaptersAttachmentsPost(file)
            return model.id.toString()
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
                    autoTestsApi.adaptersAutoTestsIdWorkItemsPost(id, WorkItemIdApiModel(workItemId))
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

    private fun searchTestResultsInRun(testRunUuid: String, configurationId: String): List<TestResultShortResponse> {
        val filter = TestResultsFilterApiModel(
            testRunIds = listOf(UUID.fromString(testRunUuid)),
            configurationIds = listOf(UUID.fromString(configurationId)),
        )
        return testResultsApi.adaptersTestResultsSearchPost(testResultsFilterApiModel = filter)
    }

    override fun getTestFromTestRun(testRunUuid: String, configurationId: String): List<String> {
        return searchTestResultsInRun(testRunUuid, configurationId)
            .mapNotNull { it.autotestExternalId }
    }

    override fun getTestResultIdByExternalId(
        testRunUuid: String,
        configurationId: String,
        externalId: String,
    ): UUID? {
        return searchTestResultsInRun(testRunUuid, configurationId)
            .firstOrNull { it.autotestExternalId == externalId }
            ?.id
    }

    override fun getTestResult(uuid: UUID): TestResultResponse {
        return testResultsApi.adaptersTestResultsIdGet(uuid)
    }

    override fun updateTestResult(uuid: UUID, model: TestResultUpdateRequest) {
        val escapedModel = HtmlEscapeUtils.escapeHtmlInObject(model) ?: model
        testResultsApi.adaptersTestResultsIdPut(uuid, escapedModel)
    }

    override fun unlinkAutoTestToWorkItem(testId: String, workItemId: String): Boolean {
        for (i in 1..MAX_TRIES) {
            try {
                autoTestsApi.adaptersAutoTestsIdWorkItemsDelete(testId, workItemId)
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
    override fun getTestRun(uuid: String): TestRunApiResult {
        return testRunsApi.adaptersTestRunsIdGet(UUID.fromString(uuid))
    }

    @Synchronized
    override fun completeTestRun(uuid: String) {
        testRunsApi.adaptersTestRunsIdCompletePost(UUID.fromString(uuid))
    }

    @Synchronized
    override fun updateAutoTest(model: AutoTestUpdateApiModel) {
        val escapedModel = HtmlEscapeUtils.escapeHtmlInObject(model) ?: model
        autoTestsApi.adaptersAutoTestsPut(escapedModel)
    }

    @Synchronized
    override fun createAutoTest(model: AutoTestCreateApiModel): String {
        val escapedModel = HtmlEscapeUtils.escapeHtmlInObject(model) ?: model
        return requireNotNull(autoTestsApi.adaptersAutoTestsPost(escapedModel).id.toString())
    }

    @Synchronized
    override fun getAutoTestByExternalId(externalId: String): AutoTestApiResult? {
        val projectIds = hashSetOf(UUID.fromString(clientConfiguration.projectId))
        val externalIds = hashSetOf(externalId)
        val filter = AutoTestFilterApiModel(
            projectIds = projectIds,
            isDeleted = false,
            externalIds = externalIds
        )

        val includes = AutoTestSearchIncludeApiModel(INCLUDE_STEPS, INCLUDE_LINKS, INCLUDE_LABELS)

        val model = AutoTestSearchApiModel(filter, includes)

        val tests = autoTestsApi.adaptersAutoTestsSearchPost(autoTestSearchApiModel = model)

        if (tests.isEmpty()) {
            return null
        }

        return tests[0]
    }
}
