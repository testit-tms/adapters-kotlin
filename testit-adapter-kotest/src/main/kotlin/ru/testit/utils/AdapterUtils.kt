package ru.testit.utils

import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.test.TestCase
import org.slf4j.Logger
import ru.testit.listener.TestItReporter
import ru.testit.services.ExecutableTest

object AdapterUtils {

    fun debug(logger: Logger): (String, Any?) -> Unit = { format: String, arg: Any? ->
        if (logger.isDebugEnabled) logger.debug(format, arg);
    }

    fun refreshExecutableTest(executableTest: ThreadLocal<ExecutableTest>): ExecutableTest {
        executableTest.remove()
        return executableTest.get()
    }

    fun isBeforeTestRegistered(testCase: TestCase): Boolean {
        return getBeforeTestExtension(testCase) != null
    }

    fun isAfterTestRegistered(testCase: TestCase): Boolean {
        return getAfterTestExtension(testCase) != null
    }

    private fun getBeforeTestExtension(testCase: TestCase): BeforeTestListener? {
        return getSomeExtension(testCase, { item -> item as BeforeTestListener }) as BeforeTestListener?
    }

    private fun getAfterTestExtension(testCase: TestCase): AfterTestListener? {
        return getSomeExtension(testCase, { item -> item as AfterTestListener }) as AfterTestListener?
    }

    /**
     * Retrieves an extension from the test case using the provided function.
     */
    private fun getSomeExtension(testCase: TestCase, f: (item: Any) -> Extension): Extension? {
        val extensions = testCase.spec.registeredExtensions()
        if (extensions.isEmpty()) { return null }
        for (item in extensions) {
            try {
                if (isTestItReporter(item)) continue
                return f(item)
            }
            catch (_: Exception) {}
        }
        return null
    }

    /**
     * Checks if the given item is the TestItReporter interface.
     */
    private fun isTestItReporter(item: Any): Boolean {
        try {
            val it = item as TestItReporter
            return true
        } catch (_: Exception) { return false }
    }
}