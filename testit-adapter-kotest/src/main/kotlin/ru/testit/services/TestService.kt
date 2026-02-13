package ru.testit.services

import io.kotest.common.TestPath
import io.kotest.core.config.ProjectConfiguration
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.engine.test.names.DefaultDisplayNameFormatter
import io.kotest.engine.test.names.FallbackDisplayNameFormatter
import io.kotest.engine.test.names.formatTestPath
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory
import ru.testit.listener.Consumers
import ru.testit.models.ItemStatus
import ru.testit.utils.AdapterUtils
import ru.testit.utils.getContext
import ru.testit.utils.isStepContainer
import java.util.concurrent.ConcurrentHashMap

class TestService (
    private val adapterManager: AdapterManager,
    @VisibleForTesting
    internal val uuids: ConcurrentHashMap<TestPath, String>,
    private val executableTestService: ExecutableTestService
) {
    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private val formatter = FallbackDisplayNameFormatter(
        DefaultDisplayNameFormatter(ProjectConfiguration().apply {
        includeTestScopeAffixes = true
    }))

    private val debug = AdapterUtils.debug(LOGGER)

    fun onTestStart(testCase: TestCase, uuid: String) {
        val fullName: String
        if (testCase.parent?.name?.testName == "") fullName = formatter.format(testCase)
        else fullName = formatter.formatTestPath(testCase, " / ")
        val spaceName : String = testCase.spec.javaClass.packageName
        val className: String = testCase.spec.javaClass.simpleName
        // var methodName: String = testCase.name.testName
        // why wildcart? Because in kotlin with gradle there is an issue with running
        // by test name in many notations
        val externalKey = "\"$spaceName.$className.*"
        val result = ru.testit.models.TestResultCommon(
            uuid = uuid,
            className = className,
            name = testCase.name.testName,
            spaceName = spaceName,
            externalId = Utils.genExternalID(fullName),
            externalKey = externalKey,
            labels = Utils.defaultLabels(),
            tags = Utils.defaultTags(),
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
    fun stopTestWithResult(testCase: TestCase, result: TestResult) {
        val isContainer = testCase.type.name == "Container"
        val isStepContainer = isContainer && testCase.isStepContainer()
        val executableTest = executableTestService.getTest()
        var path = testCase.descriptor.path()
        if (path.value == "") path = TestPath(testCase.descriptor.id.value)
        val uuid = uuids[path] ?: "Unknown test ${testCase.descriptor}"
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
            return onTestIgnored(testCase, result.errorOrNull)
        if (result is TestResult.Failure || result is TestResult.Error)
            return onTestFailed(testCase, result.errorOrNull!!)
    }


    private fun onTestSuccessful(testCase: TestCase) {
        debug("Test successful: {}", testCase.name)
        executableTestService.setAfterStatus()
        stopTestCase(executableTestService.getUuid(), null, ItemStatus.PASSED)
    }

    private fun onTestIgnored(testCase: TestCase, cause: Throwable?) {
        debug("Test ignored: {}", testCase.name)
        executableTestService.onTestIgnoredRefreshIfNeed()
        stopTestCase(executableTestService.getUuid(), cause, ItemStatus.SKIPPED);
    }

    private fun onTestFailed(testCase: TestCase, cause: Throwable) {
        debug("Test failed: {}", testCase.name)
        if (!executableTestService.isTestStatus()) {
            return;
        }
        executableTestService.setAfterStatus();
        stopTestCase(executableTestService.getUuid(), cause, ItemStatus.FAILED);
    }

    fun stopTestCase(uuid: String, throwable: Throwable?, status: ItemStatus) {
        adapterManager.updateTestCase(uuid, Consumers.setStatus(status, throwable))
        adapterManager.stopTestCase(uuid)
    }




}
