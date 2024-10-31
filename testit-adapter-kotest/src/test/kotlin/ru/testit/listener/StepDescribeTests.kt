// package ru.testit.listener

// import io.kotest.core.Tuple2
// import io.kotest.core.spec.AfterTest
// import io.kotest.core.spec.style.DescribeSpec
// import io.kotest.core.test.TestCase
// import io.kotest.core.test.TestResult
// import io.kotest.matchers.should
// import io.kotest.matchers.shouldBe
// import ru.testit.models.LinkItem
// import ru.testit.models.LinkType
// import ru.testit.models.TestItContext
// import ru.testit.utils.*

// /***/

// // isStepContainer true, 2 tests, before(failed) + after(failed): failed both
// class BeforeAfterFailed : DescribeSpec({
//     extensions(TestItReporter(true))
//     beforeTest {
//         "beforeTest" shouldBe "Failed"
//     }
//     // executes even if beforeTest is failed
//     afterTest {
//         it.testItAfterTest {
//             "afterTest" shouldBe "Failed"
//         }
//     }
//     it("before failed, test1 body pass -> failed") {
//         3 + 4 shouldBe 7
//     }
//     it("before failed, test2 body failed -> failed") {
//         "TestBody" shouldBe "Failed"
//     }
// })

// // isStepContainer true, 2 tests, before + after: ok + failed
// class BeforeAfterPassed : DescribeSpec({
//     extensions(TestItReporter(true))
//     beforeTest {
//     }
//     // executes even if beforeTest is failed
//     afterTest {
//         it.testItAfterTest {
//         }
//     }
//     it("test1 passed") {
//         3 + 4 shouldBe 7
//     }
//     it("test2 failed") {
//         "TestBody" shouldBe "Failed"
//     }
// })

// // isStepContainer true, 2 tests, before + after(failed): failed
// class BeforePassedAfterPassed : DescribeSpec({
//     extensions(TestItReporter(true))
//     beforeTest {
//     }
//     // executes even if beforeTest is failed
//     afterTest {
//         it.testItAfterTest {
//             "afterTest" shouldBe "failed"
//         }
//     }
//     it("test1 passed, afterTest failed -> failed") {
//         3 + 4 shouldBe 7
//     }
//     it("test2 failed, afterTest failed -> failed") {
//         "TestBody" shouldBe "Failed"
//     }
// })

//  /**/
// // isStepContainer true, 1 test, 2 steps before + after(failed): ok + failed
// class BeforeAfterSteps : DescribeSpec({
//     extensions(TestItReporter(true))

//     beforeTest {
//         it.setSetupName("beforeTest")
//     }

//     // executes even if beforeTest is failed
//     afterTest {
//         it.a.setTeardownName("afterTest")
//         it.testItAfterTest {
//         }
//     }

//     describe("describe step container test") {
//         testCase.asStepContainer()
//         testCase.setContext(
//             TestItContext(
//                 links = mutableListOf(
//                     LinkItem(url = "https://google.com", title = "",
//                     description = "", type = LinkType.BLOCKED_BY)
//                 )
//             )
//         )
//         it("an inner step1 - failed") {
//             "Step1" shouldBe "Failed"
//         }
//         it("an inner step2 - pass") {
//             3 + 4 shouldBe 7
//         }
//     }
// })


// // isStepContainer false, 2 tests, before + after(failed): ok + failed
// class BeforeAfterStepsContainerFalse : DescribeSpec({
//     extensions(TestItReporter(isStepContainers = false))

//     beforeTest {
//         it.setSetupName("beforeTest")
//     }

//     // executes even if beforeTest is failed
//     afterTest {
//         it.testItAfterTest("afterTest", {
//             1 shouldBe 1
//         })
//     }

//     describe("describe step container test") {
//         testCase.asStepContainer() // doesn't work because isStepContainers false
//         testCase.setContext(
//             TestItContext(
//                 links = mutableListOf(
//                     LinkItem(url = "https://google.com", title = "",
//                         description = "", type = LinkType.BLOCKED_BY)
//                 )
//             )
//         )
//         it("test1 - failed") {
//             "Step1" shouldBe "Failed"
//         }
//         it("test2 - pass") {
//             3 + 4 shouldBe 7
//         }
//     }
// })
