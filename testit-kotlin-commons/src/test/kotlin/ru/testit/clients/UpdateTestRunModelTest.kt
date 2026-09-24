package ru.testit.clients

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.testit.clients.Converter.Companion.toModel
import ru.testit.kotlin.adaptersapi.models.AttachmentApiResult
import ru.testit.kotlin.adaptersapi.models.LinkApiResult
import ru.testit.kotlin.adaptersapi.models.LinkType
import ru.testit.kotlin.adaptersapi.models.TestRunApiResult
import ru.testit.kotlin.adaptersapi.models.TestRunState
import ru.testit.kotlin.adaptersapi.models.TestStatusApiResult
import ru.testit.kotlin.adaptersapi.models.TestStatusApiType
import ru.testit.kotlin.adaptersapi.models.UpdateLinkApiModel
import ru.testit.properties.TestRunMetadataParser
import java.util.UUID

class UpdateTestRunModelTest {

    @Test
    fun `toModel preserves description launchSource links attachments and tags`() {
        val linkId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        val attachmentId = UUID.fromString("11111111-2222-3333-4444-555555555555")
        val testRun = sampleTestRun(
            links = listOf(
                LinkApiResult(
                    url = "https://existing.example",
                    type = LinkType.Related,
                    id = linkId,
                    title = "Old",
                )
            ),
            attachments = listOf(
                AttachmentApiResult(
                    id = attachmentId,
                    fileId = "file-1",
                    type = "text/plain",
                    propertySize = 1f,
                    name = "note.txt",
                )
            ),
            tags = listOf("smoke"),
            description = "Keep me",
            launchSource = "CI",
        )

        val model = testRun.toModel("Renamed")

        assertEquals("Renamed", model.name)
        assertEquals("Keep me", model.description)
        assertEquals("CI", model.launchSource)
        assertEquals(listOf("smoke"), model.tags)
        assertEquals(1, model.links!!.size)
        assertEquals(linkId, model.links!![0].id)
        assertEquals(1, model.attachments!!.size)
        assertEquals(attachmentId, model.attachments!![0].id)
    }

    @Test
    fun `merge keeps existing links and attachments when adding configured link`() {
        val existingLink = LinkApiResult(
            url = "https://existing.example",
            type = LinkType.Related,
            id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
        )
        val testRun = sampleTestRun(
            links = listOf(existingLink),
            attachments = listOf(
                AttachmentApiResult(
                    id = UUID.fromString("11111111-2222-3333-4444-555555555555"),
                    fileId = "file-1",
                    type = "text/plain",
                    propertySize = 1f,
                    name = "note.txt",
                )
            ),
            tags = listOf("smoke"),
            description = "Keep me",
            launchSource = "CI",
        )

        val existing = testRun.links.map {
            UpdateLinkApiModel(
                id = it.id,
                url = it.url,
                title = it.title,
                description = it.description,
                type = it.type,
            )
        }
        val configured = TestRunMetadataParser.toUpdateLinkModels(
            TestRunMetadataParser.parseLinks(
                """[{"url":"https://ci.example/job/1","title":"Job"}]"""
            )
        )
        val mergedLinks = TestRunMetadataParser.mergeUpdateLinks(existing, configured)
        val mergedTags = TestRunMetadataParser.mergeTags(testRun.tags, listOf("nightly"))
        val model = testRun.toModel(testRun.name).copy(
            links = mergedLinks,
            tags = mergedTags,
        )

        assertTrue(mergedTags.containsAll(listOf("smoke", "nightly")))
        assertEquals(2, model.links!!.size)
        assertEquals(existingLink.id, model.links!![0].id)
        assertEquals(1, model.attachments!!.size)
        assertEquals("Keep me", model.description)
        assertEquals("CI", model.launchSource)
    }

    private fun sampleTestRun(
        links: List<LinkApiResult>,
        attachments: List<AttachmentApiResult>,
        tags: List<String>,
        description: String?,
        launchSource: String?,
    ): TestRunApiResult {
        return TestRunApiResult(
            id = UUID.fromString("5819479d-e38b-40d0-9e35-c5b2dab50158"),
            name = "Old",
            stateName = TestRunState.InProgress,
            status = TestStatusApiResult(
                id = UUID.fromString("bfab6f14-7b65-4270-86ca-4adb9df741bd"),
                type = TestStatusApiType.InProgress,
                code = "INPROGRESS",
            ),
            attachments = attachments,
            links = links,
            tags = tags,
            description = description,
            launchSource = launchSource,
        )
    }
}
