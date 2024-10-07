//package ru.testit.listener
//
//import io.kotest.core.spec.style.ShouldSpec
//import io.kotest.matchers.shouldBe
//
//class ShouldTest : ShouldSpec({
//    extensions(TestItReporter(false))
//
//    context("String.length") {
//        should("return the length of the string") {
//            "sammy".length shouldBe 5
//            "".length shouldBe 0
//        }
//    }
//})