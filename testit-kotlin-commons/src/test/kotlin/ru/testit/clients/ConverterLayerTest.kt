package ru.testit.clients

import ru.testit.Helper
import ru.testit.kotlin.adaptersapi.models.LayerSource
import ru.testit.models.ItemStatus
import ru.testit.models.TestLayers
import ru.testit.models.TestResultCommon
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ConverterLayerTest {

    private val projectId = UUID.fromString(Helper.TEST_UUID)

    @Test
    fun create_includesLayerWhenSet() {
        val result = testResult(layer = TestLayers.API)

        val model = Converter.testResultToAutoTestPostModel(result, projectId)

        assertEquals(TestLayers.API, model.layer?.name)
        assertEquals(LayerSource.Run, model.layer?.source)
    }

    @Test
    fun create_omitsLayerWhenNotSet() {
        val model = Converter.testResultToAutoTestPostModel(testResult(), projectId)

        assertNull(model.layer)
    }

    @Test
    fun create_acceptsCustomLayerString() {
        val result = testResult(layer = "my-custom-layer")

        val model = Converter.testResultToAutoTestPostModel(result, projectId)

        assertEquals("my-custom-layer", model.layer?.name)
    }

    @Test
    fun update_alwaysSendsResetLayerFalse() {
        val model = Converter.testResultToAutoTestPutModel(testResult(), projectId, null)

        assertFalse(model.resetLayer)
        assertNull(model.layer)
    }

    @Test
    fun update_includesLayerWhenSet() {
        val result = testResult(layer = TestLayers.E2E)

        val model = Converter.testResultToAutoTestPutModel(result, projectId, null)

        assertFalse(model.resetLayer)
        assertEquals(TestLayers.E2E, model.layer?.name)
        assertEquals(LayerSource.Run, model.layer?.source)
    }

    @Test
    fun failedUpdatePath_includesLayerFromTest() {
        val autotest = Helper.generateAutoTestApiResult(projectId.toString())
        val result = testResult(layer = TestLayers.UI)

        val model = Converter.autoTestModelToAutoTestUpdateApiModel(
            autoTestModel = autotest,
            links = Converter.convertPutLinks(result.linkItems),
            externalKey = result.externalKey,
            isFlaky = autotest.isFlaky,
            layer = result.layer,
        )

        assertFalse(model.resetLayer)
        assertEquals(TestLayers.UI, model.layer?.name)
    }

    private fun testResult(layer: String? = null): TestResultCommon =
        Helper.generateTestResult().apply {
            this.layer = layer
            itemStatus = ItemStatus.PASSED
        }
}
