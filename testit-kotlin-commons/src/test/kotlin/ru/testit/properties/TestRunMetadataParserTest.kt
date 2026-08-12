package ru.testit.properties

import ru.testit.kotlin.adaptersapi.models.LinkType
import ru.testit.kotlin.adaptersapi.models.UpdateLinkApiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestRunMetadataParserTest {

    @Test
    fun parseTags_commaSeparated() {
        assertEquals(listOf("smoke", "nightly"), TestRunMetadataParser.parseTags("smoke, nightly"))
    }

    @Test
    fun parseTags_jsonArray() {
        assertEquals(listOf("smoke", "nightly"), TestRunMetadataParser.parseTags("""["smoke","nightly"]"""))
    }

    @Test
    fun parseTags_invalid_returnsEmpty() {
        assertTrue(TestRunMetadataParser.parseTags("""{"bad":true}""").isEmpty())
    }

    @Test
    fun parseLinks_jsonArray_defaultsTypeToRelated() {
        val links = TestRunMetadataParser.parseLinks(
            """[{"url":"https://ci.example/job/1","title":"CI Job"}]"""
        )
        assertEquals(1, links.size)
        assertEquals("https://ci.example/job/1", links[0].url)
        assertEquals("CI Job", links[0].title)

        val createModels = TestRunMetadataParser.toCreateLinkModels(links)
        assertEquals(LinkType.Related, createModels[0].type)
    }

    @Test
    fun parseLinks_skipsBlankUrl() {
        val links = TestRunMetadataParser.parseLinks("""[{"url":"  "},{"url":"https://ok"}]""")
        assertEquals(listOf("https://ok"), links.map { it.url })
    }

    @Test
    fun mergeTags_preservesExistingAndAddsNew() {
        assertEquals(
            listOf("ui", "smoke"),
            TestRunMetadataParser.mergeTags(listOf("ui"), listOf("smoke", "ui")),
        )
    }

    @Test
    fun mergeUpdateLinks_deduplicatesByUrl() {
        val existing = listOf(
            UpdateLinkApiModel(url = "https://a", type = LinkType.Related, title = "A"),
        )
        val configured = listOf(
            UpdateLinkApiModel(url = "https://a", type = LinkType.Issue, title = "dup"),
            UpdateLinkApiModel(url = "https://b", type = LinkType.Related, title = "B"),
        )
        val merged = TestRunMetadataParser.mergeUpdateLinks(existing, configured)
        assertEquals(listOf("https://a", "https://b"), merged.map { it.url })
        assertEquals("A", merged[0].title)
    }
}
