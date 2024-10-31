package ru.testit.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
annotation class Title(val value: String)