package ru.testit.utils

import io.kotest.core.Tuple2
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import ru.testit.listener.TestItReporter
import ru.testit.listener.TestItWriter
import ru.testit.models.TestItContext
import ru.testit.models.TestItParams


fun TestCase.setAfterTestException(cause: Throwable?) {
    setParameters(
        TestItParams(
            afterTestThrowable = cause
        )
    )
}

/**
 * Set [name] as a `name` property for Setup object in Test IT.
 */
fun TestCase.setSetupName(name: String) {
    setParameters(
        TestItParams(
            setupName = name
        )
    )
}

/**
 * Set [name] as a `name` property for TearDown object in Test IT.
 */
fun TestCase.setTeardownName(name: String) {
    setParameters(
        TestItParams(
            teardownName = name
        )
    )
}

fun TestCase.setupName(): String? {
    return getParams()?.setupName
}

fun TestCase.teardownName(): String? {
    return getParams()?.teardownName
}

/**
 * Set [name] as a `name` property for TearDown object in Test IT.
 *
 * Executes the provided [body] function and sets any exceptions thrown to the
 * `afterTestException` property for the testCase object. If an exception is thrown, it will be rethrown.
 * @param name the name of the TearDown object
 * @param body the function to execute
 */
fun Tuple2<TestCase, TestResult>.testItAfterTest(name: String, body: () -> Unit) {
    this.a.setTeardownName(name)
    testItAfterTest(body)
}


/**
 * Executes the provided [body] function and sets any exceptions thrown to the
 * `afterTestException` property for the testCase object. If an exception is thrown, it will be rethrown.
 * @param body the function to execute
 */
fun Tuple2<TestCase, TestResult>.testItAfterTest(body: () -> Unit) {
    try {
        body()
    }
    catch (e: Throwable ) {
        this.a.setAfterTestException(e)
        throw e
    }
}

fun TestCase.afterTestThrowable(): Throwable? {
    return getParams()?.afterTestThrowable
}

fun TestCase.setContext(value: TestItContext) {
    val writer = getWriter() ?: return
    val context = writer.context[this.name.toString()]
    if (context != null) {
        value.uuid = if (value.uuid != null) value.uuid else context.uuid
        value.externalId = if (value.externalId != null) value.externalId else context.externalId
        value.links = if (value.links != null) value.links else context.links
        value.workItemIds = if (value.workItemIds != null) value.workItemIds else context.workItemIds
        value.attachments = if (value.attachments != null) value.attachments else context.attachments
        value.name = if (value.name != null) value.name else context.name
        value.title = if (value.title != null) value.title else context.title
        value.message = if (value.message != null) value.message else context.message
        value.itemStatus = if (value.itemStatus != null) value.itemStatus else context.itemStatus
        value.description = if (value.description != null) value.description else context.description
        value.parameters = if (value.parameters != null) value.parameters else context.parameters
        value.labels = if (value.labels != null) value.labels else context.labels
    }
    writer.context[this.name.toString()] = value
}

fun TestCase.getContext(): TestItContext? {
    val writer = getWriter() ?: return null
    return writer.context[this.name.toString()]
}

/**
 * Set current test block as a step container.
 *
 * Requires [TestItReporter.isStepContainers] to be `true` to work as expected.
 */
fun TestCase.asStepContainer() {
    setParameters(
        TestItParams(
            isStepContainer = true
        ))
}

/**
 * check [TestItReporter.isStepContainers] and [TestItParams.isStepContainer] to be `true`
 */
fun TestCase.isStepContainer(isStepContainers: Boolean): Boolean {
    return isStepContainers && this.isStepContainer()
}

/**
 * check [TestItReporter.isStepContainers] and parent's [TestItParams.isStepContainer] to be `true`
 */
fun TestCase.isStep(isStepContainers: Boolean): Boolean {
    return isStepContainers && this.isStep()
}


private fun TestCase.getParams(): TestItParams? {
    val writer = getWriter() ?: return null
    return writer.params[this.name.toString()]
}

private fun TestCase.setParameters(value: TestItParams) {
    val writer = getWriter() ?: return
    val params = writer.params[this.name.toString()]
    if (params != null) {
        value.isStepContainer = if (value.isStepContainer) value.isStepContainer else params.isStepContainer
        value.afterTestThrowable = if (value.afterTestThrowable != null) value.afterTestThrowable else params.afterTestThrowable
        value.setupName = if (value.setupName != null) value.setupName else params.setupName
        value.teardownName = if (value.teardownName != null) value.teardownName else params.teardownName
    }
    writer.params[this.name.toString()] = value
}

private fun TestCase.isStepContainer(): Boolean {
    return getParams()?.isStepContainer ?: false
}

/**
 * @return `true` if `this.parent` set as Step Container using [TestCase.asStepContainer]
 */
private fun TestCase.isStep(): Boolean {
    val writer = getWriter() ?: return false
    val p = writer.params[this.parent?.name.toString()] ?: return false
    return p.isStepContainer
}

private fun TestCase.getWriter(): TestItWriter? {
    val extensions = spec.registeredExtensions()
    if (extensions.isEmpty()) { return null }
    for (item in extensions) {
        try {
            return (item as TestItReporter).writer
        }
        catch (_: Exception) {}
    }
    return null
}

