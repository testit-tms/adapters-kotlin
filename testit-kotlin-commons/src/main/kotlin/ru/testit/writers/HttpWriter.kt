package ru.testit.writers

import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.clients.Converter
import ru.testit.kotlin.adaptersapi.infrastructure.ClientException
import ru.testit.kotlin.adaptersapi.models.*
import ru.testit.models.ClassContainer
import ru.testit.models.ItemStatus
import ru.testit.models.MainContainer
import ru.testit.models.TestResultCommon
import ru.testit.services.ResultStorage
import java.util.Collections.addAll

class HttpWriter(
    private val config: ClientConfiguration,
    private val apiClient: ApiClient, private val storage:
    ResultStorage
) : Writer {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(HttpWriter::class.java)
    }

    private val testResults: MutableMap<String, UUID> = HashMap()

    override fun writeTest(testResultCommon: TestResultCommon) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Write auto test {}", testResultCommon.externalId)
            }

            upsertAutoTest(testResultCommon, null, null)

            val existingId = resolveTestResultId(testResultCommon)
            if (existingId != null) {
                testResults[testResultCommon.uuid!!] = existingId
                if (testResultCommon.itemStatus == ItemStatus.INPROGRESS) {
                    return
                }
                updateTestResult(existingId, testResultCommon, null, null)
                return
            }

            val autoTestResultsForTestRunModel = Converter.testResultToAutoTestResultsForTestRunModel(
                testResultCommon, UUID.fromString(config.configurationId))

            LOGGER.debug("send result by testRunId: " + config.testRunId)
            val ids = apiClient.sendTestResults(config.testRunId, listOf(autoTestResultsForTestRunModel))
            testResults[testResultCommon.uuid!!] = UUID.fromString(ids[0])
        } catch (e: Exception) {
            LOGGER.error("Can not write the autotest: {}", e.message)
        }
        catch (e: ClientException) {
            LOGGER.error("Can not write the autotest: {}", e.message)
            LOGGER.error("body: {}", e.response!!.getPrivateProperty("body"))
        }
    }

    fun <T : Any> T.getPrivateProperty(variableName: String): Any? {
        return javaClass.getDeclaredField(variableName).let { field ->
            field.isAccessible = true
            return@let field.get(this)
        }
    }


    private fun updateTestLinkToWorkItems(autoTestId: String, workItemIds: MutableList<String>) {
        val linkedWorkItems = apiClient.getWorkItemsLinkedToTest(autoTestId)

        for (linkedWorkItem in linkedWorkItems) {
            val linkedWorkItemId = linkedWorkItem.globalId.toString()

            if (workItemIds.contains(linkedWorkItemId)) {
                workItemIds.remove(linkedWorkItemId)
                continue
            }

            if (config.automaticUpdationLinksToTestCases) {
                apiClient.unlinkAutoTestToWorkItem(autoTestId, linkedWorkItemId)
            }
        }

        apiClient.linkAutoTestToWorkItems(autoTestId, workItemIds)
    }

    override fun writeClass(container: ClassContainer): Unit = container.children.forEach { testUuid ->
        storage.getTestResult(testUuid)?.let { test ->
            try {
                val model = apiClient.getAutoTestByExternalId(test.get().externalId!!)  ?: return

                val testResult = test.get()

                val beforeClass = Converter.convertFixture(container.beforeClassMethods, null)
                val beforeEach = Converter.convertFixture(container.beforeEachTest, testUuid)
                beforeClass.addAll(beforeEach)

                val afterClass = Converter.convertFixture(container.afterClassMethods, null)
                val afterEach = Converter.convertFixture(container.afterEachTest, testUuid)
                afterClass.addAll(afterEach)


                val autoTestUpdateApiModel = Converter.autoTestModelToAutoTestUpdateApiModel(
                    autoTestModel = model,
                    setup = beforeClass,
                    teardown = afterClass,
                    isFlaky = model.isFlaky,
                    layer = testResult.layer,
                )

                apiClient.updateAutoTest(autoTestUpdateApiModel)
            } catch (e: Exception) {
                LOGGER.error("Can not write the class: ${e.message}")
            }
        }
    }

    override fun writeTests(container: MainContainer): Unit = try {
        val beforeAll = Converter.convertFixture(container.beforeMethods, null)
        val afterAll = Converter.convertFixture(container.afterMethods, null)
        val beforeResultAll = Converter.convertResultFixture(container.beforeMethods, null)
        val afterResultAll = Converter.convertResultFixture(container.afterMethods, null)

        container.children.forEach { classUuid ->
            storage.getClassContainer(classUuid)?.let { cl ->
                val beforeResultClass = Converter.convertResultFixture(cl.get().beforeClassMethods, null)
                val afterResultClass = Converter.convertResultFixture(cl.get().afterClassMethods, null)

                for (testUuid in cl.get().children) {
                    val test = storage.getTestResult(testUuid)?.let { it }
                    if (test?.isEmpty!!) {
                        continue
                    }
                    try {
                        val testResult = test.get()

                        val beforeResultEach = Converter.convertResultFixture(cl.get().beforeEachTest, testUuid)
                        val beforeResultFinish = ArrayList(beforeResultAll).apply {
                            addAll(beforeResultClass)
                            addAll(beforeResultEach)
                        }

                        val afterResultEach = Converter.convertResultFixture(cl.get().afterEachTest, testUuid)
                        val afterResultFinish = ArrayList<AttachmentPutModelAutoTestStepResultsModel>().apply {
                            addAll(afterResultEach)
                            addAll(afterResultClass)
                            addAll(afterResultAll)
                        }

                        publishTestResultAtEnd(
                            testResult,
                            cl.get(),
                            beforeAll,
                            afterAll,
                            beforeResultFinish,
                            afterResultFinish,
                        )
                    } catch (e: Exception) {
                        LOGGER.error("Can not update the autotest: ${e.toString()}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        LOGGER.error("Error during test writing: ${e.message}")
    }

    private fun publishTestResultAtEnd(
        testResult: TestResultCommon,
        classContainer: ClassContainer,
        beforeAll: MutableList<AutoTestStepApiResult>,
        afterAll: MutableList<AutoTestStepApiResult>,
        beforeResultFinish: List<AttachmentPutModelAutoTestStepResultsModel>,
        afterResultFinish: List<AttachmentPutModelAutoTestStepResultsModel>,
    ) {
        val (beforeFinish, afterFinish) = buildAutoTestFixtures(
            testResult, classContainer, beforeAll, afterAll)

        upsertAutoTest(testResult, beforeFinish, afterFinish)

        val model = Converter.testResultToAutoTestResultsForTestRunModel(
            testResult, UUID.fromString(config.configurationId), beforeResultFinish, afterResultFinish)

        val existingId = resolveTestResultId(testResult)
        if (existingId != null && isInProgressInTms(existingId)) {
            val ids = apiClient.sendTestResults(config.testRunId, listOf(model))
            testResults[testResult.uuid!!] = UUID.fromString(ids[0])
            return
        }

        if (existingId != null) {
            updateTestResult(existingId, testResult, beforeResultFinish, afterResultFinish)
            testResults[testResult.uuid!!] = existingId
            return
        }

        val ids = apiClient.sendTestResults(config.testRunId, listOf(model))
        testResults[testResult.uuid!!] = UUID.fromString(ids[0])
    }

    private fun isInProgressInTms(testResultId: UUID): Boolean {
        val status = apiClient.getTestResult(testResultId).status ?: return false
        return status.code.equals(ItemStatus.INPROGRESS.value, ignoreCase = true)
    }

    private fun buildAutoTestFixtures(
        testResult: TestResultCommon,
        classContainer: ClassContainer,
        beforeAll: MutableList<AutoTestStepApiResult>,
        afterAll: MutableList<AutoTestStepApiResult>,
    ): Pair<MutableList<AutoTestStepApiResult>, MutableList<AutoTestStepApiResult>> {
        val testUuid = testResult.uuid!!
        val beforeFinish = ArrayList(beforeAll)
        beforeFinish.addAll(Converter.convertFixture(classContainer.beforeClassMethods, null))
        beforeFinish.addAll(Converter.convertFixture(classContainer.beforeEachTest, testUuid))

        val afterFinish = ArrayList<AutoTestStepApiResult>()
        afterFinish.addAll(Converter.convertFixture(classContainer.afterClassMethods, null))
        afterFinish.addAll(Converter.convertFixture(classContainer.afterEachTest, testUuid))
        afterFinish.addAll(afterAll)
        return beforeFinish to afterFinish
    }

    private fun upsertAutoTest(
        testResult: TestResultCommon,
        beforeFinish: MutableList<AutoTestStepApiResult>?,
        afterFinish: MutableList<AutoTestStepApiResult>?,
    ) {
        val autotest = apiClient.getAutoTestByExternalId(testResult.externalId!!)
        var autoTestId: String

        if (autotest != null) {
            val autoTestUpdateApiModel = when {
                beforeFinish != null && afterFinish != null -> {
                    Converter.autoTestModelToAutoTestUpdateApiModel(
                        autotest, beforeFinish, afterFinish, autotest.isFlaky, testResult.layer)
                }
                testResult.itemStatus == ItemStatus.FAILED -> {
                    Converter.autoTestModelToAutoTestUpdateApiModel(
                        autoTestModel = autotest,
                        links = Converter.convertPutLinks(testResult.linkItems),
                        externalKey = testResult.externalKey,
                        isFlaky = autotest.isFlaky,
                        layer = testResult.layer,
                    )
                }
                else -> {
                    Converter.testResultToAutoTestPutModel(
                        result = testResult,
                        projectId = UUID.fromString(config.projectId),
                        isFlaky = autotest.isFlaky)
                }
            }
            apiClient.updateAutoTest(autoTestUpdateApiModel)
            autoTestId = autotest.id.toString()
        } else {
            autoTestId = apiClient.createAutoTest(
                Converter.testResultToAutoTestPostModel(testResult, UUID.fromString(config.projectId)))
            if (beforeFinish != null && afterFinish != null) {
                val created = apiClient.getAutoTestByExternalId(testResult.externalId!!)!!
                apiClient.updateAutoTest(Converter.autoTestModelToAutoTestUpdateApiModel(
                    created, beforeFinish, afterFinish, created.isFlaky, testResult.layer))
            }
        }

        if (testResult.workItemIds.isNotEmpty()) {
            updateTestLinkToWorkItems(autoTestId, testResult.workItemIds)
        }
    }

    private fun resolveTestResultId(testResult: TestResultCommon): UUID? {
        testResults[testResult.uuid]?.let { return it }
        return apiClient.getTestResultIdByExternalId(
            config.testRunId,
            config.configurationId,
            testResult.externalId!!,
        )?.also { testResults[testResult.uuid!!] = it }
    }

    private fun updateTestResult(
        testResultId: UUID,
        testResult: TestResultCommon,
        beforeResultFinish: List<AttachmentPutModelAutoTestStepResultsModel>?,
        afterResultFinish: List<AttachmentPutModelAutoTestStepResultsModel>?,
    ) {
        val existing = apiClient.getTestResult(testResultId)
        val model = Converter.testResultCommonToTestResultUpdateModel(
            testResult,
            existing,
            beforeResultFinish?.let { modelToRequest(it) },
            afterResultFinish?.let { modelToRequest(it) },
        )
        apiClient.updateTestResult(testResultId, model)
    }

    fun modelToRequest(models: List<AttachmentPutModelAutoTestStepResultsModel>): List<AutoTestStepResultUpdateRequest> {
        return models.map { AutoTestStepResultUpdateRequest(
            title = it.title,
            description = it.description,
            info = it.info,
            startedOn = it.startedOn,
            completedOn = it.completedOn,
            duration = it.duration,
            outcome = it.outcome,
            stepResults = stepModelToRequest(it.stepResults),
            attachments = attachmentModelToRequest(it.attachments),
            parameters = it.parameters
        ) }
    }

    fun attachmentModelToRequest(models: List<AttachmentPutModel>?): List<AttachmentUpdateRequest>? {
        return models?.map { AttachmentUpdateRequest(
            id = it.id
        ) }
    }

    fun stepModelToRequest(models: List<AttachmentPutModelAutoTestStepResultsModel>?): List<AutoTestStepResultUpdateRequest>? {
        return models?.map { AutoTestStepResultUpdateRequest(
            title = it.title,
            description =  it.description,
            info = it.info,
            startedOn = it.startedOn,
            completedOn = it.completedOn,
            duration = it.duration,
            outcome = it.outcome,
            stepResults =  if (it.stepResults?.size!! > 0) stepModelToRequest(it.stepResults)  else emptyList(),
            attachments = attachmentModelToRequest(it.attachments)
        ) }
    }


    override fun writeAttachment(path: String): String = try {
        apiClient.addAttachment(path)
    } catch (e: Exception) {
        LOGGER.error("Can not write attachment: ${e.message}")
        ""
    }

    fun addUuid(key: String, uuid: UUID) {
        this.testResults[key] = uuid
    }

}
