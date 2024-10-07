//package ru.testit.listener
//
//import io.kotest.core.spec.style.FunSpec
//import io.kotest.matchers.shouldBe
//import ru.testit.models.Label
//import ru.testit.models.TestItContext
//import ru.testit.utils.asStepContainer
//import ru.testit.utils.setContext
//
//class StepContextTest : FunSpec({
//    extensions(TestItReporter(true, ))
//
//
//    context("fun step spec test") {
//        testCase.asStepContainer()
//        testCase.setContext(TestItContext(
//            labels = mutableListOf(Label("some label1"))
//        ))
//
//        test("shouldFail_") {
//
//            true shouldBe false
//        }
//
//        test("shouldPass") {
//            true shouldBe true
//        }
//    }
//
//})