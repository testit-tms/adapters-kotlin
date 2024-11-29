package ru.testit.writers

import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.kotlin.client.infrastructure.ClientException
import ru.testit.kotlin.client.models.AttachmentPutModelAutoTestStepResultsModel
import ru.testit.kotlin.client.models.AutoTestPutModel
import ru.testit.kotlin.client.models.AutoTestResultsForTestRunModel
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

            val autotest = apiClient.getAutoTestByExternalId(testResultCommon.externalId!!)
            val workItemIds = testResultCommon.workItemIds
            var autoTestId: String? = null

            if (autotest != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Auto test is exist. Update auto test {}", testResultCommon.externalId)
                }

                val autoTestPutModel: AutoTestPutModel
                when {
                    testResultCommon.itemStatus == ItemStatus.FAILED -> {
                        autoTestPutModel = Converter.autoTestModelToAutoTestPutModel(
                            autoTestModel = autotest,
                            links = Converter.convertPutLinks(testResultCommon.linkItems),
                            isFlaky = autotest.isFlaky)
                    }

                    else -> {
                        autoTestPutModel = Converter.testResultToAutoTestPutModel(
                            result = testResultCommon,
                            projectId = UUID.fromString(config.projectId),
                            isFlaky = autotest.isFlaky)
                    }
                }

//                autoTestPutModel.isFlaky = autotest.isFlaky
                apiClient.updateAutoTest(autoTestPutModel)
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
                val autoTestModel = apiClient.getAutoTestByExternalId(test.get().externalId!!)

                if (autoTestModel == null) return@forEach

                val beforeClass = Converter.convertFixture(container.beforeClassMethods, null)
                val beforeEach = Converter.convertFixture(container.beforeEachTest, testUuid)
                beforeClass.addAll(beforeEach)

                val afterClass = Converter.convertFixture(container.afterClassMethods, null)
                val afterEach = Converter.convertFixture(container.afterEachTest, testUuid)
                afterClass.addAll(afterEach)


                val autoTestPutModel = Converter.autoTestModelToAutoTestPutModel(
                    autoTestModel = autoTestModel,
                    setup = beforeClass,
                    teardown = afterClass,
                    isFlaky = autoTestModel.isFlaky
                )

                apiClient.updateAutoTest(autoTestPutModel)
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
                        val autoTestModel = apiClient.getAutoTestByExternalId(testResult.externalId!!) ?: return

                        val beforeFinish = ArrayList(beforeAll).apply {
                            if (autoTestModel.setup != null)
                                addAll(autoTestModel.setup!!)
                        }
                        val afterClass = Converter.convertFixture(cl.get().afterClassMethods, null)
                        val afterFinish = autoTestModel.teardown.apply {
                            addAll(afterClass)
                            addAll(afterAll)
                        }
                        val autoTestPutModel = Converter.autoTestModelToAutoTestPutModel(autoTestModel,
                            beforeFinish, afterFinish, autoTestModel.isFlaky
                        )

                        apiClient.updateAutoTest(autoTestPutModel)

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
                        val model = Converter.testResultToTestResultUpdateModel(resultModel,
                            beforeResultFinish, afterResultFinish)

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

