package ru.testit.models

/**
 * The marker interface for model objects with steps.
 */
interface ResultWithSteps {
    /**
     * Gets steps.
     *
     * @return the steps
     */
    fun getSteps(): List<StepResult>
}