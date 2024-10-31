package ru.testit.services

import ru.testit.utils.AdapterUtils

class ExecutableTestService (
    private val executableTest:  ThreadLocal<ExecutableTest>
) {
    fun setTestStatus() {
        var executable = this.executableTest.get()
        executable.setTestStatus();
    }
    fun setAfterStatus() {
        var executable = this.executableTest.get()
        executable.setAfterStatus();
    }
    fun getTest(): ExecutableTest {
        return this.executableTest.get()
    }
    fun getUuid(): String {
        return this.executableTest.get().uuid
    }

    fun refreshUuid() {
        AdapterUtils.refreshExecutableTest(executableTest);
    }

    fun onTestIgnoredRefreshIfNeed() {
        var executable = this.executableTest.get();
        if (executable.isAfter()) {
            executable = AdapterUtils.refreshExecutableTest(executableTest);
        }
        executable.setAfterStatus();
    }

    fun isTestStatus(): Boolean {
        return executableTest.get().isTest()
    }

    fun setAsFailedBy(cause: Throwable?) {
        var exec = executableTest.get()
        exec.isFailedStep = true
        exec.stepCause = cause
    }

}