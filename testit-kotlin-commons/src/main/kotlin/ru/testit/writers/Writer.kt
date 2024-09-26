package ru.testit.writers

import ru.testit.models.ClassContainer;
import ru.testit.models.MainContainer;
import ru.testit.models.TestResult;

interface Writer {
    fun writeTest(testResult: TestResult)
    fun writeClass(container: ClassContainer)
    fun writeTests(container: MainContainer)
    fun writeAttachment(path: String): String
}