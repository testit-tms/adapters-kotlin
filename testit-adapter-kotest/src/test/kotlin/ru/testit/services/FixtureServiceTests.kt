package ru.testit.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.engine.extensions.ExtensionException
import io.kotest.matchers.shouldBe
import io.mockk.*
import ru.testit.models.*
import ru.testit.utils.*
import java.util.UUID
import io.kotest.core.test.TestResult


class FixtureServiceTests : StringSpec({
    val mockAdapterManager = mockk<AdapterManager>(relaxed = true)
    val mockExecutableTestService = mockk<ExecutableTestService>(relaxed = true)
    val mockTestService = mockk<TestService>(relaxed = true)
    val fixtureService = FixtureService(mockAdapterManager, mockExecutableTestService, mockTestService)

    beforeTest {
        clearMocks(mockAdapterManager, mockExecutableTestService, mockTestService)
        mockkStatic("ru.testit.utils.ExtensionsKt") // Static mock
    }

    "onBeforeTestStart should initialize beforeFixtureUUID and call adapterManager.startPrepareFixtureEachTest" {
        val testCase = mockk<TestCase>()
        val start = System.currentTimeMillis()
        val lastClassContainerId = "classContainerId"
        val parentUuid = UUID.randomUUID().toString()

        every { mockExecutableTestService.getUuid() } returns parentUuid

        fixtureService.onBeforeTestStart(testCase, start, lastClassContainerId)

        fixtureService.beforeFixtureUUID shouldBe fixtureService.beforeFixtureUUID // Verifying UUID is set
        verify {
            mockAdapterManager.startPrepareFixtureEachTest(
                lastClassContainerId,
                fixtureService.beforeFixtureUUID!!,
                withArg { fixtureResult ->
                    fixtureResult.name shouldBe "Setup"
                    fixtureResult.start shouldBe start
                    fixtureResult.parent shouldBe parentUuid
                    fixtureResult.itemStage shouldBe ItemStage.RUNNING
                }
            )
        }
    }


    "onBeforeTestOk should mark fixture as PASSED and stop it" {

        val capturedLambda = slot<(FixtureResult) -> Unit>()
        val beforeFixtureUUID = UUID.randomUUID().toString()

        val start = System.currentTimeMillis()
        val stop = start + 1000
        fixtureService.beforeFixtureUUID = beforeFixtureUUID

        val testCase = mockk<TestCase>(relaxed = true) {
            every { setupName() } returns "CustomSetup"
        }
        every {
            mockAdapterManager.updateFixture(eq(beforeFixtureUUID), capture(capturedLambda))
        } just Runs

        every {
            mockAdapterManager.stopFixture(beforeFixtureUUID)
        } just Runs

        fixtureService.onBeforeTestOk(testCase, start, stop)

        val fixtureResult = FixtureResult(name = "Initial", start = start)
        capturedLambda.captured.invoke(fixtureResult)

        fixtureResult.itemStatus shouldBe ItemStatus.PASSED
        fixtureResult.itemStage shouldBe ItemStage.FINISHED
        fixtureResult.stop shouldBe stop
        fixtureResult.name shouldBe "CustomSetup"


        verify {
            mockAdapterManager.updateFixture(beforeFixtureUUID, any())
            mockAdapterManager.stopFixture(beforeFixtureUUID)
        }
    }

    "registerAfterTestFixture should initialize afterFixtureUUID and call adapterManager.startTearDownFixtureEachTest" {
        val testCase = mockk<TestCase>()
        val start = System.currentTimeMillis()
        val lastClassContainerId = "classContainerId"
        val parentUuid = UUID.randomUUID().toString()

        every { mockExecutableTestService.getUuid() } returns parentUuid

        fixtureService.registerAfterTestFixture(testCase, start, lastClassContainerId)

        fixtureService.afterFixtureUUID shouldBe fixtureService.afterFixtureUUID // Verifying UUID is set
        verify {
            mockAdapterManager.startTearDownFixtureEachTest(
                lastClassContainerId,
                fixtureService.afterFixtureUUID!!,
                withArg { fixtureResult ->
                    fixtureResult.start shouldBe start
                    fixtureResult.parent shouldBe parentUuid
                    fixtureResult.itemStage shouldBe ItemStage.RUNNING
                }
            )
        }
    }

    "handleFixturesFails should handle failures and update the test case appropriately" {
        mockkObject(AdapterUtils)
        every {
            AdapterUtils.isAfterTestRegistered(any())
        } returns true


        val testCase = mockk<TestCase> {
            every { isStep() } returns false
            every { afterTestThrowable() } returns null
            every { teardownName() } returns "CustomTeardown"
            every { setupName() } returns "CustomSetup"
            every { spec.rootTests()[0].name.testName } returns "SampleName"
        }
        val result = mockk<TestResult>(relaxed = true) {
            every { errorOrNull } returns ExtensionException.BeforeAnyException(Error("BeforeException"))
        }

        val beforeTestStart = System.currentTimeMillis()
        val afterTestStart = System.currentTimeMillis()

        val isFinished = fixtureService.handleFixturesFails(testCase, result, beforeTestStart, afterTestStart)

        isFinished shouldBe true
        verify {
            mockAdapterManager.updateFixture(fixtureService.beforeFixtureUUID!!, any())
            mockExecutableTestService.setAfterStatus()
            mockTestService.stopTestCase(
                any(),
                any(),
                ItemStatus.FAILED
            )
        }
    }

    "onAfterTestFailed should mark after fixture as FAILED and stop it" {

        val capturedLambda = slot<(FixtureResult) -> Unit>()
        val start = System.currentTimeMillis()
        val stop = start + 1000
        val afterFixtureUUID = UUID.randomUUID().toString()
        fixtureService.afterFixtureUUID = afterFixtureUUID
        val testCase = mockk<TestCase> {
            every { teardownName() } returns "CustomTeardown"
            every { afterTestThrowable() } returns Throwable("Test Error")
        }
        every {
            mockAdapterManager.updateFixture(eq(afterFixtureUUID), capture(capturedLambda))
        } just Runs
        every {
            mockAdapterManager.stopFixture(afterFixtureUUID)
        } just Runs

        fixtureService.onAfterTestFailed(testCase, start, stop)

        val fixtureResult = FixtureResult(name = "Initial", start = start)
        capturedLambda.captured.invoke(fixtureResult)

        fixtureResult.itemStatus shouldBe ItemStatus.FAILED
        fixtureResult.itemStage shouldBe ItemStage.FINISHED
        fixtureResult.stop shouldBe stop
        fixtureResult.name shouldBe "CustomTeardown"

        verify {
            mockAdapterManager.updateFixture(afterFixtureUUID, any())
            mockAdapterManager.stopFixture(afterFixtureUUID)
        }
    }

    "updateAfterTestTime should update start time of the after fixture" {
        val start = System.currentTimeMillis()
        val capturedLambda = slot<(FixtureResult) -> Unit>()
        val afterFixtureUUID = UUID.randomUUID().toString()
        fixtureService.afterFixtureUUID = afterFixtureUUID

        every {
            mockAdapterManager.updateFixture(eq(afterFixtureUUID), capture(capturedLambda))
        } just Runs

        fixtureService.updateAfterTestTime(start)
        val fixtureResult = FixtureResult(name = "Initial", start = start)
        capturedLambda.captured.invoke(fixtureResult)

        fixtureResult.start shouldBe start

        verify {
            mockAdapterManager.updateFixture(afterFixtureUUID, any())
        }
    }
})
