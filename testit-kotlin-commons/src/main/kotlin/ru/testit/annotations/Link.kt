package ru.testit.annotations

import ru.testit.models.LinkType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Link(
    val url: String,
    val title: String = "",
    val description: String = "",
    val type: LinkType
)