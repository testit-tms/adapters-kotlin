package ru.testit.properties

enum class AdapterMode(val value: Int) {
    DEFAULT_UNIMPLEMENTED(0),
    RUN_FROM_TESTRUN(1),
    NEW_TEST_RUN(2);

    companion object {
        fun fromValue(value: Int): AdapterMode? {
            return AdapterMode.entries.find { it.value == value }
        }
    }
}