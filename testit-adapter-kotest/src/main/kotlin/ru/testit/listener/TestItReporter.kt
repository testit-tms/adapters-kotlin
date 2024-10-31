package ru.testit.listener


import io.kotest.core.listeners.*
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlin.reflect.KClass

class TestItReporter(
    private val isStepContainers: Boolean = false,
) : BeforeTestListener, AfterTestListener, InstantiationErrorListener, ProjectListener,
    BeforeSpecListener, AfterSpecListener, BeforeInvocationListener, AfterInvocationListener,
    AfterEachListener, FinalizeSpecListener, AfterContainerListener {

    val writer = TestItWriter(isStepContainers)

    // beforeAll analogue
    override suspend fun beforeSpec(spec: Spec) {
        writer.onBeforeAll(spec)
    }

    override suspend fun afterSpec(spec: Spec) {
        writer.onAfterAll(spec)
    }


    // this handler -> beforeTest call
    override suspend fun beforeTest(testCase: TestCase) {
        writer.registerBeforeAfterExtensions(testCase)
    }

    // beforeTest() OK -> this handler -> test() -> afterInvokation,
    // otherwise -> afterTest() -> afterTest extension call,
    override suspend fun beforeInvocation(testCase: TestCase, iteration: Int): Unit {
        writer.finishBeforeTestIfExists(testCase)
        // starting test after ok beforeTest
        writer.onBeforeTestInvocation(testCase)
    }

    // test() any result -> this handler
    // -> afterTest() -> afterTest extension
    override suspend fun afterInvocation(testCase: TestCase, iteration: Int) {
        writer.onAfterTestInvocation(testCase)
    }


    // called even after "beforeTest" block
    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        val isTestFinished = writer.handleFixturesFails(testCase, result)
        if (isTestFinished) {
            return
        }
        writer.finishTestOrStep(testCase, result)
    }


    override suspend fun instantiationError(kclass: KClass<*>, t: Throwable) {
        writer.onInstantiationError(kclass, t)
    }

    override suspend fun finalizeSpec(kclass: KClass<out Spec>, results: Map<TestCase, TestResult>) {
    }

    override suspend fun beforeProject() {
    }

    override suspend fun afterProject() {
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
    }

    override suspend fun afterAny(testCase: TestCase, result: TestResult) {
    }

    override suspend fun afterContainer(testCase: TestCase, result: TestResult) {
    }
}
