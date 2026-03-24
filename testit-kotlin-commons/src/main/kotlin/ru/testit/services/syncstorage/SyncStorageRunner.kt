package ru.testit.services.syncstorage

import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Manages Sync Storage lifecycle: download binary, start process,
 * register worker, manage worker status, and send test results.
 *
 * Follows the specification from adapters-python PR #243.
 */
class SyncStorageRunner(
    private var testRunId: String,
    private val port: String = DEFAULT_PORT,
    private val baseUrl: String? = null,
    private val privateToken: String? = null
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var client: SyncStorageClient = SyncStorageClient("http://localhost:$port")
    private var process: Process? = null
    private var workerPid: String = "worker-${Thread.currentThread().id}-${System.currentTimeMillis()}"

    var isMaster: Boolean = false
        private set
    var isAlreadyInProgress: Boolean = false
    var isRunning: Boolean = false
        private set
    var isExternal: Boolean = false
        private set

    /**
     * Start Sync Storage: check if already running, otherwise download and launch.
     * Then register the worker.
     */
    fun start(): Boolean {
        try {
            if (isRunning) {
                logger.info("SyncStorage already running")
                return true
            }

            // Check if externally started
            if (client.isHealthy()) {
                logger.info("SyncStorage already running on port $port. Connecting to existing instance.")
                isRunning = true
                isExternal = true
                registerWorker()
                return true
            }

            // Download and start
            val executablePath = prepareExecutable()
            val command = buildCommand(executablePath)

            logger.info("Starting SyncStorage: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
                .directory(File(executablePath).parentFile)
                .redirectErrorStream(true)

            process = processBuilder.start()

            // Read output in background
            Thread({
                try {
                    process?.inputStream?.bufferedReader()?.forEachLine { line ->
                        logger.info("SyncStorage: $line")
                    }
                } catch (e: Exception) {
                    logger.debug("SyncStorage output reader stopped: ${e.message}")
                }
            }, "sync-storage-output-reader").apply { isDaemon = true }.start()

            // Wait for startup
            if (!waitForStartup(STARTUP_TIMEOUT_SECONDS)) {
                throw RuntimeException("SyncStorage failed to start within ${STARTUP_TIMEOUT_SECONDS}s")
            }

            isRunning = true
            logger.info("SyncStorage started on port $port")

            Thread.sleep(2000) // Brief wait for stabilization (matches Python impl)
            registerWorker()

            return true
        } catch (e: Exception) {
            logger.error("Error starting SyncStorage: ${e.message}", e)
            return false
        }
    }

    /**
     * Register this worker with Sync Storage.
     */
    private fun registerWorker() {
        try {
            val request = RegisterRequest(pid = workerPid, testRunId = testRunId)
            val response = client.registerWorker(request)
            isMaster = response.isMaster

            if (isMaster) {
                logger.info("Registered as MASTER worker, pid=$workerPid")
            } else {
                logger.info("Registered as worker, pid=$workerPid")
            }
        } catch (e: Exception) {
            logger.error("Error registering worker: ${e.message}", e)
        }
    }

    /**
     * Set worker status (in_progress / completed).
     */
    fun setWorkerStatus(status: String) {
        if (!isRunning) return
        try {
            val request = SetWorkerStatusRequest(
                pid = workerPid,
                status = status,
                testRunId = testRunId
            )
            client.setWorkerStatus(request)
            logger.debug("Set worker status to $status")
        } catch (e: Exception) {
            logger.warn("Error setting worker status: ${e.message}")
        }
    }

    /**
     * Send an in-progress test result to Sync Storage (only if master).
     */
    fun sendInProgressTestResult(model: TestResultCutApiModel): Boolean {
        if (!isMaster) {
            logger.debug("Not master, skipping sendInProgressTestResult")
            return false
        }
        if (isAlreadyInProgress) {
            logger.debug("Already in progress, skipping duplicate send")
            return false
        }

        val success = client.sendInProgressTestResult(testRunId, model)
        if (success) {
            isAlreadyInProgress = true
        }
        return success
    }

    /**
     * Update test run ID (e.g. after creation).
     */
    fun updateTestRunId(newTestRunId: String) {
        this.testRunId = newTestRunId
    }

    /**
     * Stop the Sync Storage process if we started it.
     */
    fun stop() {
        if (!isExternal && process != null) {
            try {
                process?.destroyForcibly()
                logger.info("SyncStorage process stopped")
            } catch (e: Exception) {
                logger.warn("Error stopping SyncStorage: ${e.message}")
            }
        }
        client.close()
        isRunning = false
    }

    // --- Private helpers ---

    private fun buildCommand(executablePath: String): List<String> {
        val cmd = mutableListOf(executablePath)
        testRunId.let { cmd.addAll(listOf("--testRunId", it)) }
        cmd.addAll(listOf("--port", port))
        baseUrl?.let { cmd.addAll(listOf("--baseURL", it)) }
        privateToken?.let { cmd.addAll(listOf("--privateToken", it)) }
        return cmd
    }

    private fun waitForStartup(timeoutSeconds: Int): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            if (client.isHealthy()) return true
            Thread.sleep(1000)
        }
        return false
    }

    private fun prepareExecutable(): String {
        val fileName = getFileNameForPlatform()
        val cachesDir = Paths.get(System.getProperty("user.dir"), "build", ".caches")
        Files.createDirectories(cachesDir)
        val targetPath = cachesDir.resolve(fileName)

        if (Files.exists(targetPath)) {
            logger.info("Using cached SyncStorage binary: $targetPath")
            targetPath.toFile().setExecutable(true)
            return targetPath.toString()
        }

        val downloadUrl = "$SYNC_STORAGE_REPO_URL$SYNC_STORAGE_VERSION/$fileName"
        logger.info("Downloading SyncStorage from $downloadUrl")

        URL(downloadUrl).openStream().use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        targetPath.toFile().setExecutable(true)
        logger.info("Downloaded SyncStorage binary to $targetPath")
        return targetPath.toString()
    }

    private fun getFileNameForPlatform(): String {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        val osPart = when {
            "win" in osName -> "windows"
            "mac" in osName || "darwin" in osName -> "darwin"
            "linux" in osName -> "linux"
            else -> throw RuntimeException("Unsupported OS: $osName")
        }

        val archPart = when {
            "amd64" in osArch || "x86_64" in osArch -> "amd64"
            "aarch64" in osArch || "arm64" in osArch -> "arm64"
            else -> throw RuntimeException("Unsupported architecture: $osArch")
        }

        val name = "syncstorage-$SYNC_STORAGE_VERSION-${osPart}_$archPart"
        return if (osPart == "windows") "$name.exe" else name
    }

    companion object {
        const val SYNC_STORAGE_VERSION = "v0.1.18"
        const val SYNC_STORAGE_REPO_URL = "https://github.com/testit-tms/sync-storage-public/releases/download/"
        const val DEFAULT_PORT = "49152"
        const val STARTUP_TIMEOUT_SECONDS = 30
    }
}
