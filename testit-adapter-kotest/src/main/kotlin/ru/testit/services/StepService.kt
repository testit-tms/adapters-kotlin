package ru.testit.services

import io.kotest.common.TestPath
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.slf4j.LoggerFactory
import ru.testit.listener.Consumers
import ru.testit.models.ItemStatus
import ru.testit.models.StepResult
import ru.testit.utils.AdapterUtils
import java.util.concurrent.ConcurrentHashMap

class StepService (
    private val adapterManager: AdapterManager,
    private val uuids: ConcurrentHashMap<TestPath, String>,
    private val executableTestService: ExecutableTestService,
    private val isStepContainers: Boolean
) {
    private val LOGGER = LoggerFactory.getLogger(javaClass)


    private val debug = AdapterUtils.debug(LOGGER)


    /**
     * Initialize step with [ExecutableTest.uuid] as parent
     */
    fun onStepStart(step: TestCase) {
        if (LOGGER.isDebugEnabled) {
            LOGGER.debug(
                "Intercept step: {}", step.name
            )
        }

        var testName = step.spec.rootTests()[0].name.testName
        println(testName)

        val parentUuid = executableTestService.getUuid()
        val stepUuid = getStepUuid(step)
        uuids[step.descriptor.path()] = stepUuid

        var stepResult = StepResult(
            name = step.name.testName,
            start = System.currentTimeMillis(),
        )
        adapterManager.startStep(parentUuid, stepUuid, stepResult);
    }


    /**
     * @see [updateStepAndStop]
     * @see [setStepParentCaseFailed]
     */
    suspend fun stopStepWithResult(step: TestCase, result: TestResult) {
        debug("Intercept step stop: {}", step.name)
//        var testName = step.spec.rootTests()[0].name.testName
        var stepUuid = uuids[step.descriptor.path()]!!
//        val stepContext = step.getContext()
        if (result is TestResult.Success) {
            debug("Step successful: {}", step.name)
            return updateStepAndStop(stepUuid, ItemStatus.PASSED, null)
        }
        if (result is TestResult.Ignored) {
            debug("Step ignored: {}", step.name)
            return updateStepAndStop(stepUuid, ItemStatus.SKIPPED, result.errorOrNull!!)
        }
        if (result is TestResult.Failure || result is TestResult.Error) {
            debug("Step failed: {}", step.name)
            updateStepAndStop(stepUuid, ItemStatus.FAILED, result.errorOrNull!!)
            setStepParentCaseFailed(result.errorOrNull!!)

        }
    }

    /**
     * Use [ExecutableTestService.setAsFailedBy] with step's cause for test's cause and status update
     */
    private suspend fun setStepParentCaseFailed(cause: Throwable) {
        val parentUuid = executableTestService.getUuid()
        adapterManager.updateTestCase(parentUuid, Consumers.setStatus(ItemStatus.FAILED, cause))
        executableTestService.setAsFailedBy(cause)
    }

    private fun getStepUuid(step: TestCase): String {
        val stepPath: String = step.parent!!.name.testName +
                step.name.testName
        return Utils.getHash(stepPath)
    }


    private fun updateStepAndStop(uuid: String, status: ItemStatus, cause: Throwable?) {
        adapterManager.updateStep(uuid, Consumers.setStepStatus(status, cause))
        adapterManager.stopStep(uuid);
    }


}