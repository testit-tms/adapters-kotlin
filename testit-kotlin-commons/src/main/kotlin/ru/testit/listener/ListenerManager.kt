package ru.testit.listener

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.testit.models.TestResult
import java.util.function.BiConsumer



class ListenerManager(val listeners: List<AdapterListener>) {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ListenerManager::class.java)
    }

    fun beforeTestStop(result: TestResult) {
        runSafelyMethod(listeners,
            BiConsumer<AdapterListener, TestResult> { obj: AdapterListener, result: TestResult? -> obj.beforeTestStop(result) },
            result)
    }

    protected fun <T : DefaultListener, S> runSafelyMethod(
        listeners: List<T>,
        method: BiConsumer<T, S>,
        objectO: S
    ) {
        listeners.forEach { listener ->
            try {
                method.accept(listener, objectO)
            } catch (e: Exception) {
                LOGGER.error("Could not invoke listener method", e)
            }
        }
    }
}