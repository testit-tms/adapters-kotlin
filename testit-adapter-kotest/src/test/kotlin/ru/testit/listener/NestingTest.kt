//package ru.testit.listener
//
//import io.kotest.core.spec.style.FunSpec
//import io.kotest.datatest.withData
//import io.kotest.matchers.shouldBe
//import ru.testit.models.Label
//import ru.testit.models.TestItContext
//import ru.testit.utils.beforeTest
//import ru.testit.utils.setContext
//

// TODO: [v] setup teardown names
// TODO: deploy api & adapter
// TODO: test context parameters
// TODO: workitems ids??


//// TODO: ---- 1. setup teardown
//// TODO: 2. abort steps after previous failed
//// TODO: x3. parametrized tests (with steps?)
//// TODO: x4. steps as callable functions ???
//// TODO: 5. test all context TestCase parameters
//
//
//data class PythagTriple(val a: Int, val b: Int, val c: Int)
//fun isPythagTriple(a: Int, b: Int, c: Int): Boolean = a * a + b * b == c * c
//
//
//class NestingTest : FunSpec({
//    val testit = TestItReporter(true)
//    extensions(testit)
//
//    beforeTest {
////        it.beforeTest()
//        it.setContext(
//            TestItContext(
//            title = "another triple test",
//            description = "another description"
//        ))
//
//    }
//
//    context("Pythag triples tests") {
//        withData(
//            PythagTriple(3, 4, 5),
//            PythagTriple(6, 8, 10),
//            PythagTriple(8, 15, 17),
//            PythagTriple(7, 24, 25),
//
//            ) { (a, b, c) ->
//            testCase.setContext(TestItContext(
//                name = "isPythagTriple($a, $b, $c)",
//                labels = mutableListOf(Label("123"))
//            ))
//            isPythagTriple(a, b, c) shouldBe true
//        }
//    }
//
//
////    context("each service should support all http methods") {
////
////        val services = listOf(
////            "http://internal.foo",
////            "http://internal.bar",
////            "http://public.baz",
////        )
////
////        val methods = listOf("GET", "POST", "PUT")
////
////        withData(services) { service ->
////            withData(methods) { method ->
////                // test service against method
////                true shouldBe true
////            }
////        }
////
////    }
//})
