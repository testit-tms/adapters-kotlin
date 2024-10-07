//package ru.testit.listener
//
//import io.kotest.core.spec.style.DescribeSpec
//import io.kotest.matchers.shouldBe
//import ru.testit.models.Label
//import ru.testit.models.TestItContext
//import ru.testit.utils.setContext
//
//class RegularTests : DescribeSpec({
//    extensions(TestItReporter(false))
//
//
//    describe("another outer test") {
//
//        it("1 + 2 shouldBe 3") {
//            testCase.setContext(TestItContext(
//                labels = mutableListOf(Label("some label1"))
//            ))
//            1 + 2 shouldBe 3
//        }
//
//        it("3 + 4 shouldBe 8") {
//            testCase.setContext(TestItContext(
//                externalId = "some_externalId"
//            ))
//            3 + 4 shouldBe 8
//        }
//
//
//    }
//
//})