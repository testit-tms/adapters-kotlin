# TestIT Kotlin Integrations
The repository contains new versions of adaptors for Kotlin test frameworks.

## Compatibility of Adapters and TMS

| Test IT | Kotest        |
|---------|---------------|
| 5.0     | 0.1.0         |
| 5.2     | 0.2.0         |
| 5.3     | 0.5.1-TMS-5.3 |
| 5.4     | 0.6.3-TMS-5.4 |
| 5.5     | 0.7.1-TMS-5.5 |
| 5.6     | 0.8.0-TMS-5.6 |
| Cloud   | 0.9.0 +       |

1. For current versions, see the releases tab. 
2. Starting with 5.2, we have added a TMS postscript, which means that the utility is compatible with a specific enterprise version. 
3. If you are in doubt about which version to use, check with the support staff. support@yoonion.ru

Supported test frameworks :
 1. [Kotest](https://kotest.io/docs/framework/framework.html)

## What's new in 1.0.0?

- New logic with a fix for test results loading
- Added sync-storage subprocess usage for worker synchronization on port **49152** by defailt.

### How to run 1.0+ locally?

You can change nothing, it's full compatible with previous versions of adapters for local run on all OS.


### How to run 1.0+ with CI/CD?

For CI/CD pipelines, we recommend starting the sync-storage instance before the adapter and waiting for its completion within the same job.

You can see how we implement this [here.](https://github.com/testit-tms/adapters-kotlin/tree/main/.github/workflows/test.yml#L84) 

1. Create an empty test run using `testit-cli` or use an existing one, and save the `testRunId`.
2. Start **sync-storage** with the correct parameters as a background process (alternatives to nohup can be used). Stream the log output to the `service.log` file:
```bash
nohup .caches/syncstorage-linux-amd64 --testRunId ${{ env.TMS_TEST_RUN_ID }} --port 49152 \
    --baseURL ${{ env.TMS_URL }} --privateToken ${{ env.TMS_PRIVATE_TOKEN }}  > service.log 2>&1 & 
```
3. Start the adapter using adapterMode=1 or adapterMode=0 for the selected testRunId.
4. Wait for sync-storage to complete background jobs by calling:
```bash
curl -v http://127.0.0.1:49152/wait-completion?testRunId=${{ env.TMS_TEST_RUN_ID }} || true
```
5. You can read the sync-storage logs from the service.log file.

#### 🚀 Warning
- If value from @WorkItemIds annotation not found in TMS then test result will NOT be uploaded.


## Metadata of autotest

Use metadata to specify information about autotest.


Description of metadata:

* `externalId` - unique internal autotest ID (used in Test IT)
* `links` - links listed in the autotest card
* `workItemIds` - a value that links autotests with manual tests. Receives the array of manual tests' IDs
* `attachments` - autotests attachments list
* `name` - internal autotest name (used in Test IT)
* `title` - autotest name specified in the autotest card. If not specified, the name from the displayName method is used
* `message` - autotest message
* `itemStatus` - autotest itemStatus 
* `description` - autotest description specified in the autotest card
* `labels` - labels listed in the autotest card
* `tags` - tags listed in the autotest card


All autotest metadata described with `TestItContext` class using `testCase.setContext()`: 

```kotlin
data class TestItContext (
    var uuid: String? = null,
    var externalId: String? = null,
    var links: MutableList<LinkItem>? = null,
    var workItemIds: MutableList<String>? = null,
    var attachments: MutableList<String>? = null,
    var name: String? = null,
    var title: String? = null,
    var message: String? = null,
    var itemStatus: ItemStatus? = null,
    var description: String? = null,
    var parameters: MutableMap<String, String>? = null,
    var labels: MutableList<Label>? = null,
    var tags: MutableList<String>? = null,
)
```

Example: 

```kotlin
 testCase.setContext(TestItContext(
    name = "isPythagTriple($a, $b, $c)",
    labels = mutableListOf(Label("123"))
))
```



## Examples

### Default example:

```kotlin
package org.example.tests

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import ru.testit.listener.TestItReporter
import ru.testit.models.LinkItem
import ru.testit.models.LinkType
import ru.testit.models.TestItContext
import ru.testit.utils.*

// isStepContainer true, 2 tests, before + after(failed): failed
class BeforePassedAfterPassed : DescribeSpec({
    // enable steps by true parameter
    extensions(TestItReporter(true))
    beforeTest {
    }
    // executes even if beforeTest is failed
    afterTest {
        it.testItAfterTest {
            "afterTest" shouldBe "failed"
        }
    }
    it("test1 passed, afterTest failed -> failed") {
        3 + 4 shouldBe 7
    }
    it("test2 failed, afterTest failed -> failed") {
        "TestBody" shouldBe "Failed"
    }
})

```


### Steps container example

```kotlin
package org.example.tests

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import ru.testit.listener.TestItReporter
import ru.testit.models.LinkItem
import ru.testit.models.LinkType
import ru.testit.models.TestItContext
import ru.testit.utils.*

// isStepContainer true, 1 test, 2 steps before + after(failed): ok + failed
class BeforeAfterSteps : DescribeSpec({
    extensions(TestItReporter(true))

    beforeTest {
        it.setSetupName("beforeTest")
    }

    // executes even if beforeTest is failed
    afterTest {
        it.a.setTeardownName("afterTest")
        it.testItAfterTest {
        }
    }

    describe("describe step container test") {
        testCase.asStepContainer()
        testCase.setContext(
            TestItContext(
                links = mutableListOf(
                    LinkItem(url = "https://google.com", title = "",
                    description = "", type = LinkType.BLOCKED_BY)
                )
            )
        )
        it("an inner step1 - failed") {
            "Step1" shouldBe "Failed"
        }
        it("an inner step2 - pass") {
            3 + 4 shouldBe 7
        }
    }
})
```

### Step container with FunSpec style

```kotlin
package org.example.tests

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.testit.listener.TestItReporter
import ru.testit.models.Label
import ru.testit.models.TestItContext
import ru.testit.utils.asStepContainer
import ru.testit.utils.setContext

class StepContextTest : FunSpec({
   extensions(TestItReporter(true, ))


   context("fun step spec test") {
       testCase.asStepContainer()
       testCase.setContext(TestItContext(
           labels = mutableListOf(Label("some label1"))
       ))

       test("shouldFail_") {
           true shouldBe false
       }

       test("shouldPass") {
           true shouldBe true
       }
   }

})

```

### Parametrized test example

```kotlin
package org.example.tests

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
   val testit = TestItReporter(true)
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
```
