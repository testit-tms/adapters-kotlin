package ru.testit.services

import java.util.UUID

data class ExecutableTest(
    val uuid: String = UUID.randomUUID().toString(),
    var isFailedStep: Boolean = false,
    var stepCause: Throwable? = null,
    var executableTestStage: ExecutableTestStage = ExecutableTestStage.BEFORE
) {
    fun setTestStatus() {
        this.executableTestStage = ExecutableTestStage.TEST
    }

    fun setAfterStatus() {
        this.executableTestStage = ExecutableTestStage.AFTER
    }

    fun isAfter(): Boolean {
        return executableTestStage == ExecutableTestStage.AFTER
    }

    fun isBefore(): Boolean {
        return executableTestStage == ExecutableTestStage.BEFORE
    }

    fun isTest(): Boolean {
        return executableTestStage == ExecutableTestStage.TEST
    }
}


/**
 * The stage of executable test.
 */
enum class ExecutableTestStage {
    BEFORE,
    TEST,
    AFTER
}