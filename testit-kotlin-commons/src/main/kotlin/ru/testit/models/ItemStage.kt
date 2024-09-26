package ru.testit.models

enum class ItemStage(val value: String) {
    RUNNING("running"),
    FINISHED("finished"),
    SCHEDULED("scheduled"),
    PENDING("pending");
}