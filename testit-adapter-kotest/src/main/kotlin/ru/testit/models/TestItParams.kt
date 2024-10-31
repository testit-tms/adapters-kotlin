package ru.testit.models

data class TestItParams (
    var isStepContainer: Boolean = false,
    var afterTestThrowable: Throwable? = null,
    var setupName: String? = null,
    var teardownName: String? = null,
) {}