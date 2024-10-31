package ru.testit.services

import io.kotest.common.TestPath
import io.kotest.core.config.ProjectConfiguration
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.engine.test.names.DefaultDisplayNameFormatter
import io.kotest.engine.test.names.FallbackDisplayNameFormatter
import io.kotest.engine.test.names.formatTestPath
import org.slf4j.LoggerFactory
import ru.testit.listener.Consumers
import ru.testit.models.ItemStatus
import ru.testit.utils.AdapterUtils
import ru.testit.utils.getContext
import java.util.concurrent.ConcurrentHashMap

class TestService (
    private val adapterManager: AdapterManager,
    private val uuids: ConcurrentHashMap<TestPath, String>,
    private val isStepContainers: Boolean,
    private val executableTestService: ExecutableTestService
) {
    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private val formatter = FallbackDisplayNameFormatter(
        DefaultDisplayNameFormatter(ProjectConfiguration().apply {
        includeTestScopeAffixes = true
    }))

    private val debug = AdapterUtils.debug(LOGGER)


    suspend fun onTestStart(testCase: TestCase, uuid: String) {
        val fullName: String = formatter.formatTestPath(testCase, " / ")

        val result = ru.testit.models.TestResult(
            uuid = uuid,
            className = testCase.spec::class.simpleName!!,
            name = testCase.name.testName,
            spaceName = testCase.spec::class.java.`package`.name,
            externalId = Utils.genExternalID(fullName),
            labels = Utils.defaultLabels(),
            linkItems = Utils.defaultLinks()
        )
        uuids[testCase.descriptor.path()] = uuid
        adapterManager.scheduleTestCase(result)
        adapterManager.startTestCase(uuid)
    }

    /**
     * Handles test's success / failure result, and steps if it's a step container.
     * @see onTestFailed
     * @see onTestIgnored
     * @see onTestSuccessful
     */
    suspend fun stopTestWithResult(testCase: TestCase, result: TestResult) {
        var isContainer = testCase.type.name == "Container"
        var isStepContainer = isContainer && isStepContainers
        var executableTest = executableTestService.getTest()

        val uuid = uuids[testCase.descriptor.path()] ?: "Unknown test ${testCase.descriptor}"
        val context = testCase.getContext()
        if (context != null) {
            adapterManager.updateTestCase(uuid, Consumers.setContext(context))
        }

        if (isStepContainer && executableTest.isFailedStep) {
            val stepCause = executableTest.stepCause
            return onTestFailed(testCase, stepCause!!)
        }
        if (result is TestResult.Success)
            return onTestSuccessful(testCase)
        if (result is TestResult.Ignored)
            return onTestIgnored(testCase, result.errorOrNull!!)
        if (result is TestResult.Failure || result is TestResult.Error)
            return onTestFailed(testCase, result.errorOrNull!!)
    }




    private suspend fun onTestSuccessful(testCase: TestCase) {
        debug("Test successful: {}", testCase.name)
        executableTestService.setAfterStatus()
        stopTestCase(executableTestService.getUuid(), null, ItemStatus.PASSED)
    }

    private suspend fun onTestIgnored(testCase: TestCase, cause: Throwable) {
        debug("Test ignored: {}", testCase.name)
        executableTestService.onTestIgnoredRefreshIfNeed()
        stopTestCase(executableTestService.getUuid(), cause, ItemStatus.SKIPPED);
    }

    private suspend fun onTestFailed(testCase: TestCase, cause: Throwable) {
        debug("Test failed: {}", testCase.name)
        if (!executableTestService.isTestStatus()) {
            return;
        }
        executableTestService.setAfterStatus();
        stopTestCase(executableTestService.getUuid(), cause, ItemStatus.FAILED);
    }

    suspend fun stopTestCase(uuid: String, throwable: Throwable?, status: ItemStatus) {
        adapterManager.updateTestCase(uuid, Consumers.setStatus(status, throwable))
        adapterManager.stopTestCase(uuid)
    }




}