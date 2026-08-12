package ru.testit.properties

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import ru.testit.kotlin.adaptersapi.models.CreateLinkApiModel
import ru.testit.kotlin.adaptersapi.models.LinkType
import ru.testit.kotlin.adaptersapi.models.UpdateLinkApiModel
import ru.testit.models.TestRunLinkConfig

object TestRunMetadataParser {
    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    fun parseTags(raw: String?): List<String> {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) {
            return emptyList()
        }
        val trimmed = raw.trim()
        return try {
            when {
                trimmed.startsWith("[") -> {
                    val array = json.decodeFromString<JsonArray>(trimmed)
                    array.mapNotNull { element ->
                        when (element) {
                            is JsonPrimitive -> element.content.trim().takeIf { it.isNotEmpty() }
                            else -> null
                        }
                    }
                }
                trimmed.startsWith("{") -> {
                    log.warn("Invalid testRunTags value: expected array or comma-separated list")
                    emptyList()
                }
                else -> trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            log.warn("Invalid testRunTags value, ignoring: {}", e.message)
            emptyList()
        }
    }

    fun parseLinks(raw: String?): List<TestRunLinkConfig> {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) {
            return emptyList()
        }
        return try {
            json.decodeFromString<List<TestRunLinkConfig>>(raw.trim())
                .filter { it.url.isNotBlank() }
                .also { links ->
                    if (links.isEmpty()) {
                        log.warn("testRunLinks parsed to empty list (missing urls?)")
                    }
                }
        } catch (e: Exception) {
            log.warn("Invalid testRunLinks JSON, ignoring: {}", e.message)
            emptyList()
        }
    }

    fun toCreateLinkModels(links: List<TestRunLinkConfig>): List<CreateLinkApiModel> =
        links.map { link ->
            CreateLinkApiModel(
                url = link.url,
                title = link.title,
                description = link.description,
                type = resolveLinkType(link.type),
            )
        }

    fun toUpdateLinkModels(links: List<TestRunLinkConfig>): List<UpdateLinkApiModel> =
        links.map { link ->
            UpdateLinkApiModel(
                url = link.url,
                title = link.title,
                description = link.description,
                type = resolveLinkType(link.type),
            )
        }

    fun mergeTags(existing: List<String>?, configured: List<String>): List<String> =
        ((existing ?: emptyList()) + configured).distinct()

    fun mergeUpdateLinks(
        existing: List<UpdateLinkApiModel>?,
        configured: List<UpdateLinkApiModel>,
    ): List<UpdateLinkApiModel> {
        val result = (existing ?: emptyList()).toMutableList()
        val existingUrls = result.map { it.url }.toHashSet()
        for (link in configured) {
            if (existingUrls.add(link.url)) {
                result.add(link)
            }
        }
        return result
    }

    private fun resolveLinkType(raw: String?): LinkType {
        if (raw.isNullOrBlank()) return LinkType.Related
        return LinkType.decode(raw) ?: run {
            log.warn("Unknown link type '{}', using Related", raw)
            LinkType.Related
        }
    }
}
