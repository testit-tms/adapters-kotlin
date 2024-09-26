package ru.testit.listener

import ru.testit.models.TestResult

interface AdapterListener : DefaultListener {
    fun beforeTestStop(result: TestResult?) {
    }
}