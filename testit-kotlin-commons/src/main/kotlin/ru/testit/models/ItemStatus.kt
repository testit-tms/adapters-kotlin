package ru.testit.models

enum class ItemStatus(val value: String) {
    PASSED("Passed"),
    FAILED("Failed"),
    SKIPPED("Skipped"),
    INPROGRESS("InProgress"),
    BLOCKED("Blocked");
}