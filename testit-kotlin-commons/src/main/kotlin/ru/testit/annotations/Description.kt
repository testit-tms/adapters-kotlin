package ru.testit.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
annotation class Description(val value: String)