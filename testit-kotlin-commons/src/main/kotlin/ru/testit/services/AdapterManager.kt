package ru.testit.services


import org.slf4j.LoggerFactory
import ru.testit.clients.ApiClient
import ru.testit.clients.ClientConfiguration
import ru.testit.clients.TmsApiClient
import ru.testit.kotlin.client.infrastructure.ClientError
import ru.testit.kotlin.client.infrastructure.ClientException
import ru.testit.kotlin.client.infrastructure.ServerException
import ru.testit.kotlin.client.models.TestRunState
import ru.testit.listener.AdapterListener
import ru.testit.properties.AdapterConfig
import ru.testit.services.Adapter.getResultStorage
import ru.testit.writers.HttpWriter
import ru.testit.writers.Writer
import java.util.*
import java.util.function.Consumer
import ru.testit.listener.ListenerManager;
import ru.testit.listener.ServiceLoaderListener
import ru.testit.models.*
import ru.testit.properties.AdapterMode


class AdapterManager(private var clientConfiguration: ClientConfiguration,
                private var adapterConfig: AdapterConfig,
              private var client: ApiClient,
) {

    private var writer: Writer? = null
    private var threadContext = ThreadContext()
    private var storage = Adapter.getResultStorage()
    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private var listenerManager: ListenerManager = getDefaultListenerManager()


    constructor(clientConfiguration: ClientConfiguration, adapterConfig: AdapterConfig)
            : this(clientConfiguration, adapterConfig, getDefaultListenerManager())


    constructor(
        clientConfiguration: ClientConfiguration,
        adapterConfig: AdapterConfig,
        listenerManager: ListenerManager
    ) : this(clientConfiguration, adapterConfig, TmsApiClient(clientConfiguration)) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Client configurations: {}", clientConfiguration)
            LOGGER.debug("Adapter configurations: {}", adapterConfig)
        }

        this.storage = getResultStorage()
        this.threadContext = ThreadContext()
        this.client = TmsApiClient(this.clientConfiguration)
        this.writer = HttpWriter(this.clientConfiguration, this.client, this.storage)
        this.listenerManager = listenerManager;
    }

    /**
     * @see [TmsApiClient.createTestRun]
     */
    suspend fun startTests() {
        if (!adapterConfig.shouldEnableTmsIntegration()) {
            return
        }

        LOGGER.debug("Start launch")

        synchronized(this.clientConfiguration) {
            if (this.clientConfiguration.testRunId != "null") {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Test run is exist.")
                }
                return
            }

            try {
                val response = this.client.createTestRun()
                LOGGER.debug("set testRunId to: " + response.id.toString())
                this.clientConfiguration.testRunId = response.id.toString()
            } catch (e: Exception) {
                LOGGER.error("Can not start the launch: ${e.message}")
            }
        }
    }

    /**
     * Is not used in current version.
     */
    suspend fun stopTests() {
        if (!adapterConfig.shouldEnableTmsIntegration()) {
            return
        }

        LOGGER.debug("Stop launch")

        try {
            val testRun = this.client.getTestRun(this.clientConfiguration.testRunId)

            if (testRun.stateName != TestRunState.Completed) {
                this.client.completeTestRun(this.clientConfiguration.testRunId)
            }
        } catch (e: Exception) {
//            if (e.responseBody?.contains("the StateName is already Completed") == true) {
//                return
//            }
            LOGGER.error("Can not finish the launch: ${e.message}")
        }
    }

    suspend fun startMainContainer(container: MainContainer) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        container.start = System.currentTimeMillis()
        storage.put(container.uuid!!, container)

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start new main container {}", container)
    }

    suspend fun stopMainContainer(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val found = storage.getTestsContainer(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not stop main container: container with uuid {} not found", uuid)
            return
        }
        val container = found.get()
        container.stop = System.currentTimeMillis()

        if (LOGGER.isDebugEnabled) LOGGER.debug("Stop main container {}", container)

        writer?.writeTests(container)
    }

    suspend fun startClassContainer(parentUuid: String, container: ClassContainer) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        storage.getTestsContainer(parentUuid).ifPresent { parent ->
            synchronized(storage) {
                parent.children.add(container.uuid!!)
            }
        }
        container.start = System.currentTimeMillis()
        storage.put(container.uuid!!, container)

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start new class container {} for parent {}", container, parentUuid)
    }

    suspend fun stopClassContainer(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val found = storage.getClassContainer(uuid)
        if (!found.isPresent) {
            LOGGER.debug("Could not stop class container: container with uuid {} not found", uuid)
            return
        }
        val container = found.get()
        container.stop = System.currentTimeMillis()

        if (LOGGER.isDebugEnabled) LOGGER.debug("Stop class container {}", container)

        writer?.writeClass(container)
    }

    suspend fun updateClassContainer(uuid: String, update: Consumer<ClassContainer>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled()) LOGGER.debug("Update class container {}", uuid)

        val found = storage.getClassContainer(uuid)
        if (!found.isPresent) {
            LOGGER.debug("Could not update class container: container with uuid {} not found", uuid)
            return
        }
        val container = found.get()
        update.accept(container)
    }

    suspend fun startTestCase(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        threadContext.clear()
        val found = storage.getTestResult(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not start test case: test case with uuid {} is not scheduled", uuid)
            return
        }
        val testResult = found.get()

        testResult.setItemStage(ItemStage.RUNNING)
        testResult.start = System.currentTimeMillis()

        threadContext.start(uuid)

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start test case {}", testResult)
        }
    }

    suspend fun scheduleTestCase(result: TestResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        result.setItemStage(ItemStage.SCHEDULED)
        result.automaticCreationTestCases = adapterConfig.shouldAutomaticCreationTestCases()
        storage.put(result.uuid!!, result)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Schedule test case {}", result)
        }
    }

    suspend fun updateTestCase(update: Consumer<TestResult>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val root = threadContext.getRoot()
        if (!root.isPresent) {
            LOGGER.error("Could not update test case: no test case running")
            return
        }

        val uuid = root.get()
        updateTestCase(uuid, update)
    }

    suspend fun updateTestCase(uuid: String, update: Consumer<TestResult>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Update test case {}", uuid)
        }

        val found = storage.getTestResult(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not update test case: test case with uuid {} not found", uuid)
            return
        }
        val testResult = found.get()

        update.accept(testResult)
    }

    suspend fun stopTestCase(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val found = storage.getTestResult(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not stop test case: test case with uuid {} not found", uuid)
            return
        }
        val testResult = found.get()

        listenerManager.beforeTestStop(testResult)

        testResult.setItemStage(ItemStage.FINISHED)
        testResult.stop = System.currentTimeMillis()

        threadContext.clear()

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stop test case {}", testResult)
        }

        writer?.writeTest(testResult)
    }

    fun startPrepareFixtureAll(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start prepare all fixture {} for parent {}", result, parentUuid)

        val container = storage.getTestsContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().beforeMethods.add(result)
            }
        }

        startFixture(uuid, result)
    }

    fun startTearDownFixtureAll(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start tear down all fixture {} for parent {}", result, parentUuid)

        val container = storage.getTestsContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().afterMethods.add(result)
            }
        }

        startFixture(uuid, result)
    }

    fun startPrepareFixture(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start prepare fixture {} for parent {}", result, parentUuid)

        val container = storage.getClassContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().beforeClassMethods.add(result)
            }
        }

        startFixture(uuid, result)
    }

    fun startTearDownFixture(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start tear down fixture {} for parent {}", result, parentUuid)

        val container = storage.getClassContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().afterClassMethods.add(result)
            }
        }

        startFixture(uuid, result)
    }

    fun startPrepareFixtureEachTest(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start prepare for each test fixture {} for parent {}", result, parentUuid)

        val container = storage.getClassContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().beforeEachTest.add(result)
            }
        }

        startFixture(uuid, result)
    }

    fun startTearDownFixtureEachTest(parentUuid: String, uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Start tear down for each test fixture {} for parent {}", result, parentUuid)

        val container = storage.getClassContainer(parentUuid)
        if (container.isPresent) {
            synchronized(storage) {
                container.get().afterEachTest.add(result)
            }
        }

        startFixture(uuid, result)
    }

    private fun startFixture(uuid: String, result: FixtureResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        storage.put(uuid, result)

        result.itemStage = ItemStage.RUNNING
        result.start = System.currentTimeMillis()

        threadContext.clear()
        threadContext.start(uuid)
    }

    fun updateFixture(uuid: String, update: (FixtureResult) -> Unit) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled) LOGGER.debug("Update fixture {}", uuid)

        val found = storage.getFixture(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not update test fixture: test fixture with uuid {} not found", uuid)
            return
        }
        val fixture = found.get()

        update(fixture)
    }

    fun stopFixture(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val found = storage.getFixture(uuid)
        if (!found.isPresent) {
            LOGGER.error("Could not stop test fixture: test fixture with uuid {} not found", uuid)
            return
        }
        val fixture = found.get()

        fixture.itemStage = ItemStage.FINISHED
        fixture.stop = System.currentTimeMillis()

        storage.remove(uuid)
        threadContext.clear()

        if (LOGGER.isDebugEnabled) LOGGER.debug("Stop fixture {}", fixture)
    }

    fun startStep(uuid: String, result: StepResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val current = threadContext.getCurrent()
        if (current.isEmpty) {
            LOGGER.debug("Could not start step $result: no test case running")
            return
        }
        val parentUuid = current
        startStep(parentUuid.get(), uuid, result)
    }

    /**
     * Start a new step as child of specified parent.
     *
     * @param parentUuid the uuid of parent test case or step.
     * @param uuid       the uuid of step.
     * @param result     the step.
     */
    fun startStep(parentUuid: String, uuid: String, result: StepResult) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        result.itemStage = ItemStage.RUNNING
        result.start = System.currentTimeMillis()

        threadContext.start(uuid)

        storage.put(uuid, result)
        var parentStep = storage.getStep(parentUuid)
        if (!parentStep.isEmpty) {
            synchronized(storage) {
                parentStep.get().getSteps().add(result)
            }
        }
        else {
            // TODO: not working with fun spec context ?
            var parentTest = storage.getTestResult(parentUuid)
            synchronized(storage) {
                parentTest.get().getSteps().add(result)
            }
        }


        if (LOGGER.isDebugEnabled) LOGGER.debug("Start step $result for parent $parentUuid")
    }

    fun updateStep(update: Consumer<StepResult>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val current = threadContext.getCurrent()
        if (current.isEmpty) {
            LOGGER.debug("Could not update step: no step running")
            return
        }
        val uuid = current
        updateStep(uuid.get(), update)
    }

    fun updateStep(uuid: String, update: Consumer<StepResult>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        if (LOGGER.isDebugEnabled()) LOGGER.debug("Update step $uuid")

        val found = storage.getStep(uuid)
        if (found.isEmpty) {
            LOGGER.error("Could not update step: step with uuid $uuid not found")
            return
        }

        val step = found
        update.accept(step.get())
    }

    /**
     * Stops current running step.
     */
    fun stopStep() {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val root = threadContext.getRoot()
        val current = threadContext.getCurrent()
            .filter { it != root.get() }
        if (current.isEmpty) {
            LOGGER.debug("Could not stop step: no step running")
            return
        }
        val uuid = current
        stopStep(uuid.get())
    }

    /**
     * Stops step by given uuid.
     *
     * @param uuid the uuid of step to stop.
     */
    fun stopStep(uuid: String) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val found = storage.getStep(uuid)
        if (found.isEmpty) {
            LOGGER.error("Could not stop step: step with uuid $uuid not found")
            return
        }

        val step = found.get()
        step.itemStage = ItemStage.FINISHED
        step.stop = System.currentTimeMillis()

        storage.remove(uuid)
        threadContext.stop()

        if (LOGGER.isDebugEnabled()) LOGGER.debug("Stop step $step")
    }

    fun addAttachments(attachments: List<String>) {
        if (!adapterConfig.shouldEnableTmsIntegration()) return

        val uuids = mutableListOf<String>()
        for (attachment in attachments) {
            val attachmentId = writer!!.writeAttachment(attachment)
            if (attachmentId.isEmpty()){
                return
            }
            uuids.add(attachmentId)
        }

        val current = threadContext.getCurrent()
        if (current.isEmpty) {
            LOGGER.error("Could not add attachment: no test is running")
            return
        }

        val result = storage.getList(current.get())
        synchronized(storage) {
            result.get().addAll(uuids)
        }
    }

    fun isFilteredMode() = adapterConfig.getMode() == AdapterMode.USE_FILTER

    fun getTestFromTestRun(): List<String> {
        if (adapterConfig.shouldEnableTmsIntegration()) {
            try {

                val testsForRun = client.getTestFromTestRun(clientConfiguration.testRunId, clientConfiguration.configurationId)

                if (LOGGER.isDebugEnabled()) LOGGER.debug("List of tests from test run: $testsForRun")

                return testsForRun
            } catch (e: Exception) {
                LOGGER.error("Could not get tests from test run", e)
            }
        }

        return mutableListOf()
    }

    fun getCurrentTestCaseOrStep(): Optional<String> = threadContext.getCurrent()
    private companion object {
        @JvmStatic
        private val getDefaultListenerManager: () -> ListenerManager = {
            val classLoader = Thread.currentThread().contextClassLoader
            ListenerManager(ServiceLoaderListener.load(AdapterListener::class.java, classLoader))
        }
    }
}