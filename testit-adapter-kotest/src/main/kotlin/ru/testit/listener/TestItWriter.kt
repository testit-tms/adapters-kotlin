package ru.testit.listener


import io.kotest.common.TestPath
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType
import org.slf4j.LoggerFactory
import ru.testit.models.*
import ru.testit.services.*
import ru.testit.utils.*
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import ru.testit.models.TestResultCommon as TestItTestResult




class TestItWriter () {
    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private val debug = AdapterUtils.debug(LOGGER)

    private val adapterManager = Adapter.getAdapterManager();

    private val uuids = ConcurrentHashMap<TestPath, String>()
    val context = ConcurrentHashMap<String, TestItContext>()
    val params = ConcurrentHashMap<String, TestItParams>()

    private var lastClassContainerId: String? = null
    private var lastMainContainerId: String? = null

    private var beforeTestStart = 0L
    private var afterTestStart = 0L

    /**
     * Checks `testCase` to be valid test or [TestItReporter.isStepContainers] to be true
     */
    private val isTestOrContainersEnabled: (testCase: TestCase) -> Boolean = {
            testCase: TestCase -> testCase.type == TestType.Container
                || testCase.type == TestType.Test || testCase.type == TestType.Dynamic }

    private val executableTestService = ExecutableTestService(
        executableTest = ThreadLocal.withInitial { ExecutableTest() }
    )
    private val stepService = StepService(
        adapterManager = adapterManager,
        uuids = uuids,
        executableTestService = executableTestService
    )

    private val testService = TestService(
        adapterManager = adapterManager,
        uuids = uuids,
        executableTestService = executableTestService
    )

    private val fixtureService = FixtureService(
        adapterManager = adapterManager,
        executableTestService = executableTestService,
        testService = testService
    )


    /**
     * Used for `instantiationError` error handling
     */
    fun onInstantiationError(kclass: KClass<*>, t: Throwable) {
        var uuid = UUID.randomUUID()

        var result = TestItTestResult(
            uuid = uuid.toString(),
            className = kclass.qualifiedName!!,
            name = kclass.simpleName!!
        ).apply {
        }
        adapterManager.scheduleTestCase(result)
        adapterManager.startTestCase(uuid.toString())

        val instanceError = (t.cause as InvocationTargetException).targetException
        testService.stopTestCase(uuid.toString(), instanceError, ItemStatus.FAILED)
    }

    /**
     * @see runContainers
     */
    fun onBeforeAll(spec: Spec) {
        var rootTestName =  spec.rootTests()[0].name.testName
        debug("Before all: {}",rootTestName)
        runContainers(rootTestName)
    }

    /**
     * @see stopContainers
     */
    fun onAfterAll(spec: Spec) {
        var rootTestName =  spec.rootTests()[0].name.testName
        debug("After all: {}", rootTestName)
        stopContainers(rootTestName)
    }


    /**
     * Do nothing in step flow.
     * Checks for registered extensions and register them in Test IT.
     *
     * Interested in  [io.kotest.core.listeners.BeforeTestListener] and
     * [io.kotest.core.listeners.AfterTestListener].
     *
     * @see FixtureService.onBeforeTestStart
     * @see FixtureService.registerAfterTestFixture
     */
    fun registerBeforeAfterExtensions(testCase: TestCase) {
        if (testCase.isStep()) {
            return
        }
        executableTestService.refreshUuid()
        if (AdapterUtils.isBeforeTestRegistered(testCase)) {
            // register before test as fixture
            beforeTestStart = System.currentTimeMillis()
            fixtureService.onBeforeTestStart(testCase, beforeTestStart, lastClassContainerId!!)
        }
        if (AdapterUtils.isAfterTestRegistered(testCase)) {
            afterTestStart = System.currentTimeMillis()
            fixtureService.registerAfterTestFixture(testCase, afterTestStart, lastClassContainerId!!)
        }
    }

    /**
     * Do nothing in step flow.
     * Called in beforeTest successful flow, finish fixture
     *
     * @see FixtureService.onBeforeTestOk
     */
    fun finishBeforeTestIfExists(testCase: TestCase) {
        if (testCase.isStep()) {
            return
        }
        // write beforeTest successful results
        if (AdapterUtils.isBeforeTestRegistered(testCase)) {
            fixtureService.onBeforeTestOk(testCase,
                beforeTestStart, System.currentTimeMillis())
        }
    }

    /**
     * Do nothing in step flow.
     * Called in before afterTest flow, set afterTest's start time
     *
     * @see FixtureService.updateAfterTestTime
     */
    fun onAfterTestInvocation(testCase: TestCase) {
        if (testCase.isStep()) {
            return
        }
        if (AdapterUtils.isAfterTestRegistered(testCase)) {
            afterTestStart = System.currentTimeMillis()
            fixtureService.updateAfterTestTime(afterTestStart)
        }
    }

    /**
     * checks test type and run step or test run
     *
     * @see StepService.onStepStart
     * @see onTestStart
     */
    fun onBeforeTestInvocation(testCase: TestCase) {
        if (!isTestOrContainersEnabled(testCase)) {
            return
        }
        var isContainer = testCase.type.name == "Container"
        var isStep = testCase.isStep();
        if (isContainer) {
            debug("we are in step container", "")
        }
        if (isStep) {
            debug("we should register this as a step", "")
            return stepService.onStepStart(testCase)
        }
        // regular test or step container
        onTestStart(testCase)
    }

    /**
     * Called on `afterTest` handler. Finish test's or step's main part in Test IT.
     *
     * @see StepService.stopStepWithResult
     * @see TestService.stopTestWithResult
     */
    suspend fun finishTestOrStep(testCase: TestCase, result: TestResult): Unit {
        if (!isTestOrContainersEnabled(testCase)) {
            return
        }
        var isContainer = testCase.type.name == "Container"

        if (testCase.isStep()) {
            return stepService.stopStepWithResult(testCase, result)
        }
        // no mark in describe
        if (isContainer && !testCase.isStepContainer()) {
            return
        }
        testService.stopTestWithResult(testCase, result)
    }


    /**
     * @see FixtureService.handleFixturesFails
     */
    suspend fun handleFixturesFails(testCase: TestCase,
                                    result: TestResult
    ): Boolean {
        return fixtureService.handleFixturesFails(testCase, result, beforeTestStart, afterTestStart)
    }


    /**
     * Init new `test` and add it to [ClassContainer.children]
     *
     * @see TestService.onTestStart
     * @see AdapterManager.updateClassContainer
     */
    private fun onTestStart(testCase: TestCase) {
        debug("Intercept test: {}", testCase.name)
        val testName = testCase.spec.rootTests()[0].name.testName
//        val uuid = getExecTestWithUuid()
        executableTestService.setTestStatus()
        val uuid = executableTestService.getUuid()

        testService.onTestStart(testCase, uuid)
        adapterManager.updateClassContainer(
            Utils.getHash(testName)
        ) { container: ClassContainer -> container.children.add(uuid) }
    }


    /**
     * Create new test run.
     * Init main and class containers using [AdapterManager] api
     */
    private fun runContainers(rootTestName: String) {
        adapterManager.startTests()
        lastMainContainerId = UUID.randomUUID().toString()
        val mainContainer = MainContainer(
            uuid = lastMainContainerId
        )
        adapterManager.startMainContainer(mainContainer)
        lastClassContainerId = Utils.getHash(rootTestName)
        val classContainer = ClassContainer(
            uuid = lastClassContainerId
        )
        adapterManager.startClassContainer(lastMainContainerId!!, classContainer)
    }

    private fun stopContainers(rootTestName: String) {
        adapterManager.stopClassContainer(Utils.getHash(rootTestName))
        adapterManager.stopMainContainer(lastMainContainerId!!)
    }

    @Deprecated("don't use")
    private fun getSetupUuid(testCase: TestCase): String {
        val stepPath: String = testCase.name.testName + "Setup"
        return Utils.getHash(stepPath)
    }
}
