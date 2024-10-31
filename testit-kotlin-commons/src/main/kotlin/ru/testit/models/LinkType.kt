package ru.testit.models

enum class LinkType(val value: String) {
    RELATED("Related"),
    BLOCKED_BY("BlockedBy"),
    DEFECT("Defect"),
    ISSUE("Issue"),
    REQUIREMENT("Requirement"),
    REPOSITORY("Repository")
}