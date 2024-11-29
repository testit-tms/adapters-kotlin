package ru.testit.services

import io.kotest.common.TestPath
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.testit.models.ItemStatus
import ru.testit.models.StepResult
import java.util.concurrent.ConcurrentHashMap
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration

class StepServiceTests : StringSpec({

    val mockAdapterManager = mockk<AdapterManager>(relaxed = true)
    val mockExecutableTestService = mockk<ExecutableTestService>(relaxed = true)
    val uuids = ConcurrentHashMap<TestPath, String>()
    val stepService = StepService(mockAdapterManager, uuids, mockExecutableTestService)

    beforeTest {
        clearMocks(mockAdapterManager, mockExecutableTestService)
        uuids.clear()
    }

    "onStepStart should initialize and start a step" {
        val testCase = mockk<TestCase> {
            every { name.testName } returns "StepName"
            every { spec.rootTests()[0].name.testName } returns "TestRoot"
            every { descriptor.path() } returns TestPath("stepPath")
            every { parent?.name?.testName } returns "ParentStep"
        }
        val parentUuid = "ParentUUID"
        every { mockExecutableTestService.getUuid() } returns parentUuid

        stepService.onStepStart(testCase)

        uuids[TestPath("stepPath")] shouldBe
                uuids[TestPath("stepPath")] // Ensure UUID is added to map

        val uuid: String = uuids[TestPath("stepPath")]!!.toString()

        verify {
            mockAdapterManager.startStep(
                parentUuid,
                uuid,
                withArg { stepResult ->
                    stepResult.name shouldBe "StepName"
                    stepResult.start shouldNotBe null
                }
            )
        }
    }

    "stopStepWithResult should handle success result" {
        val testCase = mockk<TestCase> {
            every { name.testName } returns "StepName"
            every { descriptor.path() } returns TestPath("stepPath")
        }
        uuids[TestPath("stepPath")] = "StepUUID"

        val testResult = TestResult.Success(Duration.parse("10s"))

        stepService.stopStepWithResult(testCase, testResult)

        verify {
            mockAdapterManager.updateStep(
                "StepUUID",
                withArg { consumer ->
                    val stepResult = StepResult()
                    consumer.accept(stepResult)
                    stepResult.itemStatus shouldBe ItemStatus.PASSED
                    stepResult.throwable shouldBe null
                }
            )
            mockAdapterManager.stopStep("StepUUID")
        }
    }

    "stopStepWithResult should handle ignored result" {
        val testCase = mockk<TestCase> {
            every { name.testName } returns "StepName"
            every { descriptor.path() } returns TestPath("stepPath")
        }
        uuids[TestPath("stepPath")] = "StepUUID"

        val testResult = TestResult.Ignored("Step Ignored")

        stepService.stopStepWithResult(testCase, testResult)

        verify {
            mockAdapterManager.updateStep(
                "StepUUID",
                withArg { consumer ->
                    val stepResult = StepResult()
                    consumer.accept(stepResult)
                    stepResult.itemStatus shouldBe ItemStatus.SKIPPED
                }
            )
            mockAdapterManager.stopStep("StepUUID")
        }
    }

    "stopStepWithResult should handle failure result" {
        val testCase = mockk<TestCase> {
            every { name.testName } returns "StepName"
            every { descriptor.path() } returns TestPath("stepPath")
        }
        uuids[TestPath("stepPath")] = "StepUUID"

        val error = Throwable("Step Failed")
        val testResult = TestResult.Error(Duration.ZERO, error)

        stepService.stopStepWithResult(testCase, testResult)

        verify {
            mockAdapterManager.updateStep(
                "StepUUID",
                withArg { consumer ->
                    val stepResult = StepResult()
                    consumer.accept(stepResult)
                    stepResult.itemStatus shouldBe ItemStatus.FAILED
                    stepResult.throwable shouldBe error
                }
            )
            mockAdapterManager.stopStep("StepUUID")
            mockAdapterManager.updateStep(
                any(),
                withArg { consumer ->
                    val stepResult = StepResult()
                    consumer.accept(stepResult)
                    stepResult.itemStatus shouldBe ItemStatus.FAILED
                    stepResult.throwable shouldBe error
                }
            )
            mockExecutableTestService.setAsFailedBy(error)
        }
    }

    "getStepUuid should return hashed UUID based on step path" {
        val testCase = mockk<TestCase> {
            every { name.testName } returns "StepName"
            every { parent?.name?.testName } returns "ParentStep"
        }

        val stepUuid = stepService.run { getStepUuid(testCase) }
        stepUuid shouldBe Utils.getHash("ParentStepStepName")
    }
})
