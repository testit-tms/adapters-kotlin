package ru.testit.listener

import ru.testit.models.ItemStatus
import ru.testit.models.StepResult
import ru.testit.models.TestItContext
import ru.testit.models.TestResult
import java.util.Objects.nonNull
import java.util.function.Consumer

object Consumers {

    /**
     * Sets the item status and optional throwable for a test result.
     */
    fun setStatus(status: ItemStatus, throwable: Throwable?): Consumer<TestResult> {
        return Consumer<TestResult> { result: TestResult ->
            result.itemStatus = status
            if (nonNull(throwable)) {
                result.throwable = throwable
            }
        }
    }

    /**
     * Sets the item status and optional throwable for a step result.
     */
    fun setStepStatus(status: ItemStatus, throwable: Throwable?): Consumer<StepResult> {
        return Consumer<StepResult> { result: StepResult ->
            result.itemStatus = status
            if (nonNull(throwable)) {
                result.throwable = throwable
            }
        }
    }

    /**
     * Sets the context properties for a test result.
     */
    fun setContext(context: TestItContext): Consumer<TestResult> {
        return Consumer<TestResult> { result: TestResult ->
            result.externalId = context.externalId ?: result.externalId
            result.description = context.description ?: result.description
            result.workItemIds = context.workItemIds ?: result.workItemIds
            result.name = context.name ?: result.name
            result.linkItems = context.links ?: result.linkItems
            result.title = context.title ?: result.title
            result.labels = context.labels ?: result.labels
            result.message = context.message ?: result.message
            result.itemStatus = context.itemStatus ?: result.itemStatus
            result.attachments = context.attachments ?: result.attachments
            result.uuid = context.uuid ?: result.uuid
        }
    }

}