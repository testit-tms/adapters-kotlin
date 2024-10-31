package ru.testit.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
annotation class DisplayName(val value: String)