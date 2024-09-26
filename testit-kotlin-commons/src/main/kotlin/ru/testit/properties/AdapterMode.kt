package ru.testit.properties

enum class AdapterMode(val value: Int) {
    USE_FILTER(0),
    RUN_ALL_TESTS(1),
    NEW_TEST_RUN(2);

    companion object {
        fun fromValue(value: Int): AdapterMode? {
            return AdapterMode.entries.find { it.value == value }
        }
    }
}