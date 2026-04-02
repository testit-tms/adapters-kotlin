package tests

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import ru.testit.listener.TestItReporter
import ru.testit.models.Label
import ru.testit.models.TestItContext
import ru.testit.utils.setContext
import ru.testit.utils.testItAfterTest


data class PythagTriple(val a: Int, val b: Int, val c: Int)
fun isPythagTriple(a: Int, b: Int, c: Int): Boolean = a * a + b * b == c * c


class NestingTest : FunSpec({
   val testit = TestItReporter()
   extensions(testit)

   context("Pythag triples tests") {
       withData(
           PythagTriple(3, 4, 5),
           PythagTriple(6, 8, 10),
           PythagTriple(8, 15, 17),
           PythagTriple(7, 24, 25),
           PythagTriple(7, 7, 7),

           ) { (a, b, c) ->
           testCase.setContext(TestItContext(
               name = "isPythagTriple($a, $b, $c)",
               labels = mutableListOf(Label("123"))
           ))
           isPythagTriple(a, b, c) shouldBe true
       }
   }

})
