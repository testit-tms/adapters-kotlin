package ru.testit.listener

import ru.testit.models.TestResultCommon

interface AdapterListener : DefaultListener {
    fun beforeTestStop(result: TestResultCommon?) {
    }
}