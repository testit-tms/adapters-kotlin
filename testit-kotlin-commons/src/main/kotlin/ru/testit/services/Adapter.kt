package ru.testit.services


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.testit.models.LinkItem
import ru.testit.models.LinkType
import ru.testit.properties.AppProperties

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.bufferedWriter

object Adapter {
    private val LOGGER: Logger = LoggerFactory.getLogger(javaClass)
    @Volatile private var adapterManager: AdapterManager? = null
    @Volatile private var storage: ResultStorage? = null

    fun getAdapterManager(): AdapterManager {
        return adapterManager ?: synchronized(this) {
            val appProperties = AppProperties.loadProperties()
            val manager = ConfigManager(appProperties)
            adapterManager = AdapterManager(manager.getClientConfiguration(), manager.getAdapterConfig())
            adapterManager!!
        }
    }

    fun getResultStorage(): ResultStorage {
        return storage ?: synchronized(this) {
            storage = ResultStorage()
            storage!!
        }
    }

    @Deprecated("This method is no longer acceptable to compute time between versions. " +
            "Use Adapter.addLinks(String, String, String, LinkType) instead.")
    suspend fun link(title: String, description: String, type: LinkType, url: String) {
        val link = LinkItem(
            title = title,
            description = description,
            type = type,
            url = url
        )
        getAdapterManager().updateTestCase { testResult -> testResult.resultLinks.add(link) }
    }

    @Deprecated("This method is no longer acceptable to compute time between versions. " +
            "Use Adapter.addLinks(String, String, String, LinkType) instead.")
    suspend fun addLink(url: String, title: String, description: String, type: LinkType) {
        val link = LinkItem(
            title =title,
            description = description,
            type = type,
            url = url
        )

        addLinks(listOf(link))
    }

    suspend fun addLinks(url: String, title: String, description: String, type: LinkType) {
        val link = LinkItem(
            title =title,
            description = description,
            type = type,
            url = url
        )

        addLinks(listOf(link))
    }

    suspend fun addLinks(links: List<LinkItem>) {
        getAdapterManager().updateTestCase { testResult -> testResult.resultLinks.addAll(links) }
    }

    fun addAttachments(attachments: List<String>) {
        getAdapterManager().addAttachments(attachments)
    }

    fun addAttachments(attachment: String) {
        addAttachments(listOf(attachment))
    }

    fun addAttachments(content: String, fileName: String?) {
        val effectiveFileName = fileName ?: (UUID.randomUUID().toString() + "-attachment.txt")
        val path = Paths.get(effectiveFileName)

        try {
            BufferedWriter(path.bufferedWriter(Charset.defaultCharset())).use { writer ->
                writer.write(content)
            }
        } catch (e: IOException) {
            LOGGER.error("Can not write file '$effectiveFileName':", e)
        }

        addAttachments(listOf(effectiveFileName))

        try {
            path.toFile().delete()
        } catch (e: IOException) {
            LOGGER.error("Can not delete file '$effectiveFileName':", e)
        }
    }

    fun addAttachments(fileName: String, inputStream: InputStream) {
        if (fileName == null) {
            LOGGER.error("Attachment name is empty")
            return
        }

        val path = Paths.get(fileName)

        try {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            LOGGER.error("Can not write file '$fileName':", e)
        }

        addAttachments(listOf(fileName))

        try {
            path.toFile().delete()
        } catch (e: IOException) {
            LOGGER.error("Can not delete file '$fileName':", e)
        }
    }

    @Deprecated("This method is no longer acceptable to compute time between versions. " +
            "Use Adapter.addAttachments(String attachment) instead.")
    fun addAttachment(attachment: String) {
        addAttachments(listOf(attachment))
    }

    suspend fun addMessage(message: String) {
        getAdapterManager().updateTestCase { testResult -> testResult.message = message }
    }
}