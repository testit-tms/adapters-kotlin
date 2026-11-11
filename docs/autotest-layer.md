# Autotest layer (test pyramid)

Specification for setting the **test pyramid layer** on an autotest card from Kotlin test code.

Layer is **not** the same as autotest tags/labels or test run tags. There is no config/env default for a whole run.

## Declaration in test code

### Kotest

Set layer via `TestItContext` before the test finishes (same pattern as other metadata):

```kotlin
import ru.testit.models.TestItContext
import ru.testit.models.TestLayers
import ru.testit.utils.setContext

test("create user") {
    testCase.setContext(TestItContext(layer = TestLayers.API))
    // ...
}
```

For JVM `@Test` methods (AnnotationSpec / mixed style), you can also use:

```kotlin
import ru.testit.annotations.Layer
import ru.testit.models.TestLayers

@Layer(TestLayers.API)
@Test
fun createUser() { /* ... */ }

@Layer("my-custom-layer")
@Test
fun customLayer() { /* ... */ }
```

Extract `@Layer` from a `Method` with `Utils.extractLayer(method, parameters)` when wiring a custom listener.

## Recommended constants

```kotlin
import ru.testit.models.TestLayers

TestLayers.E2E
TestLayers.UI
TestLayers.API
TestLayers.CONTRACT
TestLayers.INTEGRATION
TestLayers.COMPONENT
TestLayers.UNIT
```

Any other non-empty string is accepted without validation.

## API mapping

| Scenario | Adapter behaviour |
|----------|-------------------|
| Layer set in test | **Create:** send `layer: { name, source: Run }` |
| Layer set in test | **Update:** send `layer` + `resetLayer: false` |
| Layer not set | send `resetLayer: false`; do **not** send `layer` |

`source` is always **`Run`**.

## Internal flow

```
test code (@Layer / TestItContext.layer)
    → TestResultCommon.layer
    → Converter.layerToApiModel
    → AutoTestCreateApiModel.layer / AutoTestUpdateApiModel.layer + resetLayer
    → TMS
```

Failed-test minimal update path also applies `layer` from the test when present.

See also [tz-autotest-layer.md](tz-autotest-layer.md) for the cross-language contract.
