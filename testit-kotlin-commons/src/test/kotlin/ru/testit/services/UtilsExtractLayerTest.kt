package ru.testit.services

import ru.testit.annotations.Layer
import ru.testit.models.TestLayers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtilsExtractLayerTest {

    @Layer(TestLayers.API)
    fun annotatedMethod() {}

    @Layer("{env}")
    fun parameterizedMethod() {}

    @Test
    fun extractLayer_readsAnnotation() {
        val method = UtilsExtractLayerTest::class.java.getDeclaredMethod("annotatedMethod")

        assertEquals(TestLayers.API, Utils.extractLayer(method, emptyMap()))
    }

    @Test
    fun extractLayer_substitutesParameters() {
        val method = UtilsExtractLayerTest::class.java.getDeclaredMethod("parameterizedMethod")

        assertEquals("prod", Utils.extractLayer(method, mapOf("env" to "prod")))
    }

    @Test
    fun extractLayer_returnsNullWhenMissing() {
        val method = UtilsExtractLayerTest::class.java.getDeclaredMethod("extractLayer_returnsNullWhenMissing")

        assertNull(Utils.extractLayer(method, emptyMap()))
    }
}
