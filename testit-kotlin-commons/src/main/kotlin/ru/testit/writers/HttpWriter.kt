package ru.testit.writers

import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.clients.Converter
import ru.testit.kotlin.client.infrastructure.ClientException
import ru.testit.kotlin.client.models.*
import ru.testit.models.ClassContainer
import ru.testit.models.ItemStatus
import ru.testit.models.MainContainer
import ru.testit.models.TestResultCommon
import ru.testit.services.ResultStorage
import ru.testit.clients.Converter.Companion.toModel
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

            val autoTestApiResult = apiClient.getAutoTestByExternalId(testResultCommon.externalId!!)
            val workItemIds = testResultCommon.workItemIds
            var autoTestId: String? = null
            val autotest = autoTestApiResult.toModel()

            if (autotest != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Auto test is exist. Update auto test {}", testResultCommon.externalId)
                }

                val autoTestUpdateApiModel: AutoTestUpdateApiModel
                when {
                    testResultCommon.itemStatus == ItemStatus.FAILED -> {
                        autoTestUpdateApiModel = Converter.autoTestModelToAutoTestUpdateApiModel(
                            autoTestModel = autotest,
                            links = Converter.convertPutLinks(testResultCommon.linkItems),
                            isFlaky = autotest.isFlaky)
                    }

                    else -> {
                        autoTestUpdateApiModel = Converter.testResultToAutoTestPutModel(
                            result = testResultCommon,
                            projectId = UUID.fromString(config.projectId),
                            isFlaky = autotest.isFlaky)
                    }
                }

//                autoTestUpdateApiModel.isFlaky = autotest.isFlaky
                apiClient.updateAutoTest(autoTestUpdateApiModel)
                autoTestId = autotest.id.toString()
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Create new auto test {}", testResultCommon.externalId)
                }

                val model = Converter.testResultToAutoTestPostModel(testResultCommon, UUID.fromString(config.projectId))
                autoTestId = apiClient.createAutoTest(model)
            }

            if (!workItemIds.isEmpty()) {
                updateTestLinkToWorkItems(autoTestId!!, workItemIds)
            }

            val autoTestResultsForTestRunModel = Converter.testResultToAutoTestResultsForTestRunModel(
                testResultCommon, UUID.fromString(config.configurationId))

            val results: MutableList<AutoTestResultsForTestRunModel> = mutableListOf()
            results.add(autoTestResultsForTestRunModel)
            LOGGER.debug("send result by testRunId: " + config.testRunId)
            val ids = apiClient.sendTestResults(config.testRunId, results)
            testResults[testResultCommon.uuid!!] = UUID.fromString(ids[0])
        } catch (e: Exception) {
            LOGGER.error("Can not write the autotest: {}", e.message)
        }
        catch (e: ClientException) {
            LOGGER.error("Can not write the autotest: {}", e.message)
            LOGGER.error("body: {}", e.response!!.getPrivateProperty("body"))
//             Json.encodeToString(e.response..toString()))
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
                val autoTestApiResult = apiClient.getAutoTestByExternalId(test.get().externalId!!)
                val autoTestModel = autoTestApiResult.toModel()

                if (autoTestModel == null) return@forEach

                val beforeClass = Converter.convertFixture(container.beforeClassMethods, null)
                val beforeEach = Converter.convertFixture(container.beforeEachTest, testUuid)
                beforeClass.addAll(beforeEach)

                val afterClass = Converter.convertFixture(container.afterClassMethods, null)
                val afterEach = Converter.convertFixture(container.afterEachTest, testUuid)
                afterClass.addAll(afterEach)


                val autoTestUpdateApiModel = Converter.autoTestModelToAutoTestUpdateApiModel(
                    autoTestModel = autoTestModel,
                    setup = beforeClass,
                    teardown = afterClass,
                    isFlaky = autoTestModel.isFlaky
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
                        val autoTestApiResult = apiClient.getAutoTestByExternalId(testResult.externalId!!)
                        val autoTestModel = autoTestApiResult.toModel() ?: return

                        val beforeFinish = ArrayList(beforeAll).apply {
                            if (autoTestModel.setup != null)
                                addAll(autoTestModel.setup!!)
                        }
                        val afterClass = Converter.convertFixture(cl.get().afterClassMethods, null)
                        val afterFinish = autoTestModel.teardown.apply {
                            addAll(afterClass)
                            addAll(afterAll)
                        }
                        val autoTestUpdateApiModel = Converter.autoTestModelToAutoTestUpdateApiModel(autoTestModel,
                            beforeFinish, afterFinish, autoTestModel.isFlaky
                        )

                        apiClient.updateAutoTest(autoTestUpdateApiModel)

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

                        val autoTestResultsForTestRunModel =
                            Converter.testResultToAutoTestResultsForTestRunModel(
                                testResult, null, beforeResultFinish, afterResultFinish)

                        val testResultId = testResults[testResult.uuid]

                        val resultModel = apiClient.getTestResult(testResultId!!)
                        val beforeResult = modelToRequest(beforeResultFinish)
                        val afterResult = modelToRequest(afterResultFinish)

                        val model = Converter.testResultToTestResultUpdateModel(resultModel,
                            beforeResult, afterResult)

                        apiClient.updateTestResult(testResultId, model)

                    } catch (e: Exception) {
                        LOGGER.error("Can not update the autotest: ${e.toString()}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        LOGGER.error("Error during test writing: ${e.message}")
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

