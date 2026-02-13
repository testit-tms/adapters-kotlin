package ru.testit.services

import io.kotest.common.TestPath
import ru.testit.listener.Consumers
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestType
import io.mockk.*
import ru.testit.models.ItemStatus
import ru.testit.models.TestItContext
import ru.testit.models.TestResultCommon
import ru.testit.utils.getContext
import ru.testit.utils.isStepContainer
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class TestServiceTests : StringSpec({

    val mockAdapterManager = mockk<AdapterManager>(relaxed = true)
    val mockExecutableTestService = mockk<ExecutableTestService>(relaxed = true)
    val mockTestCase = mockk<TestCase>(relaxed = true)

    val uuids = ConcurrentHashMap<TestPath, String>()
    val testService_ = TestService(mockAdapterManager, uuids, mockExecutableTestService)
    val testService = spyk<TestService>(testService_)

    beforeTest {
        clearMocks(mockAdapterManager, mockExecutableTestService)
        uuids.clear()
        mockkStatic("ru.testit.utils.ExtensionsKt") // Static mock
    }

    "should start a test correctly" {
        // Arrange
        val uuid = "test-uuid"
        val testName = "Test Name"
        val className = "Spec\$Subclass1"
        val packageName = "io.kotest.core.spec"
        val expectedFullName = "Test Name"

        every { mockTestCase.name.testName } returns testName
        mockkObject(Utils.Companion)
        every { Utils.genExternalID(expectedFullName) } returns "generated-id"
        every { Utils.defaultLabels() } returns mutableListOf()
        every { Utils.defaultTags() } returns mutableListOf()
        every { Utils.defaultLinks() } returns mutableListOf()
        every { mockTestCase.descriptor.path() } returns TestPath("test-path")

        // Act
        testService.onTestStart(mockTestCase, uuid)

        // Assert
        verify {
            mockAdapterManager.scheduleTestCase(
                withArg<TestResultCommon> {
                    it.uuid shouldBe uuid
                    it.name shouldBe testName
                    it.className shouldBe className
                    it.spaceName shouldBe packageName
                    it.externalId shouldBe "generated-id"
                }
            )
            mockAdapterManager.startTestCase(uuid)
        }
        uuids[TestPath("test-path")] shouldBe uuid
    }

    "should stop a test with success result" {
        // Arrange
        val uuid = "test-uuid"
        val mockResult = TestResult.success(0)
        val testName = "Test Name"

        every { mockTestCase.descriptor.path() } returns TestPath("test-path")
        every { uuids[mockTestCase.descriptor.path()] } returns uuid
        every { mockExecutableTestService.getTest() } returns mockk(relaxed = true)
        every { mockTestCase.isStepContainer() } returns false
        every { mockTestCase.name.testName } returns testName
        every { mockTestCase.descriptor.id.value } returns "test-path"
        every { mockExecutableTestService.getUuid() } returns uuid
        testService.uuids[TestPath(mockTestCase.descriptor.id.value)] = uuid

        // Act
        testService.stopTestWithResult(mockTestCase, mockResult)

        // Assert
        verify { mockAdapterManager.updateTestCase(uuid, any()) }
        verify { mockAdapterManager.stopTestCase(uuid) }
    }

    "should handle a failed test result" {
        // Arrange
        val uuid = "test-uuid"
        val exception = RuntimeException("Test failure")
        val mockResult = spyk<TestResult>(TestResult.Error(Duration.ZERO, exception))

        every { uuids[mockTestCase.descriptor.path()] } returns uuid
        every { mockExecutableTestService.getTest() } returns mockk(relaxed = true)
        every { mockTestCase.isStepContainer() } returns false
        every { mockExecutableTestService.isTestStatus() } returns true
        every { mockExecutableTestService.getUuid() } returns uuid
        every { mockResult.errorOrNull } returns exception


        // Act
        testService.stopTestWithResult(mockTestCase, mockResult)

        // Assert
        verify { mockAdapterManager.updateTestCase(uuid,
            withArg { consumer ->
                val result = TestResultCommon()
                consumer.accept(result)
                result.itemStatus shouldBe ItemStatus.FAILED
                result.throwable shouldBe  exception
            })
        }
        verify { mockAdapterManager.stopTestCase(uuid) }
    }

    "should handle ignored test result" {
        // Arrange
        val uuid = "test-uuid"
        val mockResult = spyk(TestResult.Ignored("Test ignored"))

        every { uuids[mockTestCase.descriptor.path()] } returns uuid
        every { mockExecutableTestService.getTest() } returns mockk(relaxed = true)
        every { mockTestCase.isStepContainer() } returns false
        every { mockExecutableTestService.getUuid() } returns uuid
        every { mockResult.errorOrNull } returns null


        // Act
        testService.stopTestWithResult(mockTestCase, mockResult)

        // Assert
        verify {
            mockAdapterManager.updateTestCase(uuid,
                withArg { consumer ->
                    val result = TestResultCommon()
                    consumer.accept(result)
                    result.itemStatus shouldBe ItemStatus.SKIPPED
                    result.throwable shouldBe null
                }
            )
            mockAdapterManager.stopTestCase(uuid)
        }
    }

    "should not stop a test if it's a step container and failed" {
        // Arrange
        val uuid = "test-uuid"
        val exception = RuntimeException("Step failed")
        val mockResult = TestResult.Error(Duration.ZERO, exception)

        val testCase = mockk<TestCase> {
            every { name.testName } returns "TestName"
            every { descriptor.id.value } returns "test-path"
            every { type.name } returns TestType.Container.toString()
            every { isStepContainer() } returns true
            every { getContext() } returns null
        }

        val mockExecutableTest = mockk<ExecutableTest>(relaxed = true)
        every { mockExecutableTest.isFailedStep } returns true
        every { mockExecutableTest.stepCause } returns exception

        every { mockExecutableTestService.isTestStatus() } returns true
        every { mockExecutableTestService.getTest() } returns mockExecutableTest
        every {mockExecutableTestService.getUuid() } returns uuid

        every { uuids[testCase.descriptor.path()] } returns uuid
        every { testCase.descriptor.path() } returns TestPath("")


        // Act
        testService.stopTestWithResult(testCase, mockResult)

        // Assert
        verify {
            mockAdapterManager.updateTestCase(uuid,
                withArg { consumer ->
                    val result = TestResultCommon()
                    consumer.accept(result)
                    result.itemStatus shouldBe ItemStatus.FAILED
                    result.throwable shouldBe exception
                }
            )
            mockAdapterManager.stopTestCase(uuid)
        }
    }
})
