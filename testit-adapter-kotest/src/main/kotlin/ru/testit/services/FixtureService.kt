package ru.testit.services

import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.engine.extensions.ExtensionException
import org.slf4j.LoggerFactory
import ru.testit.models.ClassContainer
import ru.testit.models.FixtureResult
import ru.testit.models.ItemStage
import ru.testit.models.ItemStatus
import ru.testit.utils.*
import java.util.*

class FixtureService (
    private val adapterManager: AdapterManager,
    private val executableTestService: ExecutableTestService,
    private val testService: TestService,
    private val isStepContainers: Boolean
) {
    var beforeFixtureUUID: String? = null
    var afterFixtureUUID: String? = null

    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private val debug = AdapterUtils.debug(LOGGER)


    /**
     * For tests only. Updates [beforeFixtureUUID], start new `beforeTest` fixture.
     * @see AdapterManager.startPrepareFixtureEachTest
     */
    fun onBeforeTestStart(testCase: TestCase, start: Long, lastClassContainerId: String) {
        debug("Before test started: ", null)

        var parentUuid = executableTestService.getUuid()
        var fixtureResult = FixtureResult(
            name = "Setup",
            start = start,
            parent = parentUuid,
            itemStage = ItemStage.RUNNING
        )
        beforeFixtureUUID = UUID.randomUUID().toString()
        adapterManager.startPrepareFixtureEachTest(lastClassContainerId, beforeFixtureUUID!!, fixtureResult);
    }


    fun onBeforeTestOk(testCase: TestCase, start: Long, stop: Long) {
        var beforeTestTime = stop - start
        debug("Before test executed successfully for: {} ms", beforeTestTime.toString())
        var setupName = testCase.setupName() ?: "Setup"
        adapterManager.updateFixture(beforeFixtureUUID!!) { result: FixtureResult ->
            result.itemStatus = ItemStatus.PASSED
            result.itemStage = ItemStage.FINISHED
            result.stop = stop
            result.name = setupName
        }
        adapterManager.stopFixture(beforeFixtureUUID!!);
    }

    fun registerAfterTestFixture(testCase: TestCase, start: Long?, lastClassContainerId: String) {
        debug("After test registered: ", null)

        var parentUuid = executableTestService.getUuid()
        var fixtureResult = FixtureResult(
//            name = "TearDown",
            start = start,
            parent = parentUuid,
            itemStage = ItemStage.RUNNING
        )
        afterFixtureUUID = UUID.randomUUID().toString()
        adapterManager.startTearDownFixtureEachTest(lastClassContainerId, afterFixtureUUID!!, fixtureResult);
    }

    /**
     * Called before correct afterTest execution, set start value
     */
    fun updateAfterTestTime(start: Long?) {
        debug("After test started: ", null)
        adapterManager.updateFixture(afterFixtureUUID!!) { result: FixtureResult ->
            result.start = start
        }
    }

    /**
     * For tests only. Handles both fixture fails if exists, afterTest and beforeTest. If
     * beforeTest failed, then init and finish test because actual will not start at all.
     * @see updateTestCaseWithFixtureFailure
     * @return true if it's need to abort afterTest execution, else false
     */
    suspend fun handleFixturesFails(testCase: TestCase, result: TestResult,
                                    beforeTestStart: Long, afterTestStart: Long): Boolean {
        if (testCase.isStep(isStepContainers)) {
            return false
        }
        var isAfterTestFailed = false
        var isBeforeTestFailed = false
        var afterTestCause: Throwable? = null
        var beforeTestCause: Throwable? = null

        if (AdapterUtils.isAfterTestRegistered(testCase)) {
            if (testCase.afterTestThrowable() != null) {
                isAfterTestFailed = true
                afterTestCause = testCase.afterTestThrowable()
                onAfterTestFailed(testCase, afterTestStart, System.currentTimeMillis())
            }
            else {
                onAfterTestOk(testCase, afterTestStart, System.currentTimeMillis())
            }
        }
        if (result.errorOrNull is ExtensionException.BeforeAnyException) {
            // call test finished with error
            isBeforeTestFailed = true
            beforeTestCause = result.errorOrNull
            onBeforeTestFailed(testCase, beforeTestStart, System.currentTimeMillis())
        }
        if (isAfterTestFailed || isBeforeTestFailed) {
            updateTestCaseWithFixtureFailure(
                testCase,
                isBeforeTestFailed,
                isAfterTestFailed,
                beforeTestCause,
                afterTestCause
            )
            return true
        }
        return false
    }


    private fun onAfterTestFailed(testCase: TestCase, start: Long, stop: Long) {
        var exception = testCase.afterTestThrowable()
        var tearDownName = testCase.teardownName() ?: "TearDown"
        debug("After test finished with error: {}", exception.toString())
        adapterManager.updateFixture(afterFixtureUUID!!) { result: FixtureResult ->
            result.itemStatus = ItemStatus.FAILED
            result.itemStage = ItemStage.FINISHED
            result.stop = stop
            result.name = tearDownName
        }
        // TODO: test and comment if not needed.
        adapterManager.stopFixture(afterFixtureUUID!!);
    }


    private fun onAfterTestOk(testCase: TestCase, start: Long, stop: Long) {
        var afterTestTime = stop - start
        debug("After test executed successfully for: {} ms", afterTestTime.toString())
        var tearDownName = testCase.teardownName() ?: "TearDown"
        adapterManager.updateFixture(afterFixtureUUID!!) { result: FixtureResult ->
            result.itemStatus = ItemStatus.PASSED
            result.itemStage = ItemStage.FINISHED
            result.stop = stop
            result.name = tearDownName
        }
        adapterManager.stopFixture(afterFixtureUUID!!);
    }


    private fun onBeforeTestFailed(testCase: TestCase, start: Long, stop: Long) {
        var beforeTestTime = stop - start
        debug("Before test executed with error for: {} ms", beforeTestTime.toString())
        var setupName = testCase.setupName() ?: "Setup"
        adapterManager.updateFixture(beforeFixtureUUID!!) { result: FixtureResult ->
            result.itemStatus = ItemStatus.FAILED
            result.itemStage = ItemStage.FINISHED
            result.stop = stop
            result.name = setupName
        }
        adapterManager.stopFixture(beforeFixtureUUID!!);
    }



    /**
     * For tests only. Handles both fixture fails if exists, afterTest and beforeTest. If
     * beforeTest failed, then init and finish test because actual will not start at all.
     * Set beforeTest/afterTest's cause to test's cause
     */
    private suspend fun updateTestCaseWithFixtureFailure(testCase: TestCase,
                                                         isBeforeTestFailed: Boolean,
                                                         isAfterTestFailed: Boolean,
                                                         beforeCause: Throwable?,
                                                         afterCause: Throwable?) {
        executableTestService.setTestStatus()
        var testCaseUuid = executableTestService.getUuid()
        var testName = testCase.spec.rootTests()[0].name.testName

        // if before test failed -> there is no testCase initialized
        if (isBeforeTestFailed) {
            testService.onTestStart(testCase, testCaseUuid)
            adapterManager.updateClassContainer(
                Utils.getHash(testName)
            ) { container: ClassContainer -> container.children.add(testCaseUuid) }
        }
        var cause = beforeCause ?: afterCause
        executableTestService.setAfterStatus();
        testService.stopTestCase(testCaseUuid, cause, ItemStatus.FAILED);
    }


}