package ru.testit.services

import ru.testit.annotations.*
import ru.testit.models.Label
import ru.testit.models.LinkItem

import java.lang.reflect.Method
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class Utils {

companion object {
//    fun toIndentedString(o: Any?): String {
//        return if (o == null) "null" else o.toString().replace("\n", "\n    ")
//    }


    fun defaultLabels(): MutableList<Label> = mutableListOf()
    fun defaultTags(): MutableList<String> = mutableListOf()
    fun genExternalID(fullName: String): String = getHash(fullName)
    fun defaultLinks(): MutableList<LinkItem> = mutableListOf()

    fun extractExternalID(atomicTest: Method, parameters: Map<String, String>): String {
        val annotation = atomicTest.getAnnotation(ExternalId::class.java)
        return if (annotation != null) setParameters(
            annotation.value,
            parameters
        ) else getHash(atomicTest.declaringClass.name + atomicTest.name)
    }


    fun extractLinks(atomicTest: Method, parameters: Map<String, String>): List<LinkItem> {
        val links = mutableListOf<LinkItem>()

        atomicTest.getAnnotation<Links>(Links::class.java)?.let { linksAnnotation ->
            for (link in linksAnnotation.links) {
                links.add(makeLink(link, parameters))
            }
        } ?: atomicTest.getAnnotation<Link>(Link::class.java)?.let {
            links.add(makeLink(it, parameters))
        }

        return links
    }


    fun extractClassname(atomicTest: Method, className: String, parameters: Map<String, String>): String {
        val annotations = listOf(
            atomicTest.getAnnotation(Classname::class.java),
            atomicTest.declaringClass.getAnnotation(Classname::class.java)
        ).filterNotNull()

        return if (annotations.isNotEmpty()) setParameters(annotations[0].value, parameters) else setParameters(
            className,
            parameters
        )
    }

    fun extractNamespace(atomicTest: Method, namespace: String, parameters: Map<String, String>): String {
        val annotations = listOf(
            atomicTest.getAnnotation(Namespace::class.java),
            atomicTest.declaringClass.getAnnotation(Namespace::class.java)
        ).filterNotNull()

        return if (annotations.isNotEmpty()) setParameters(annotations[0].value, parameters) else setParameters(
            namespace,
            parameters
        )
    }

    fun extractDescription(atomicTest: Method, parameters: Map<String, String>): String {
        val annotation = atomicTest.getAnnotation(Description::class.java)
        return if (annotation != null) setParameters(annotation.value, parameters) else ""
    }

    fun extractTitle(atomicTest: Method, parameters: Map<String, String>): String {
        val annotation = atomicTest.getAnnotation(Title::class.java)
        return if (annotation != null) setParameters(annotation.value, parameters) else atomicTest.name
    }

    fun toIndentedString(o: Any?): String {
        return o?.toString()?.replace("\n", "\n    ") ?: "null"
//    return if (o == null) "null" else o.toString().replace("\n", "\n    ")
    }

    private fun makeLink(linkAnnotation: Link, parameters: Map<String, String>): LinkItem {
        return LinkItem(
            title = setParameters(linkAnnotation.title, parameters),
            description = setParameters(linkAnnotation.description, parameters),
            url = setParameters(linkAnnotation.url, parameters),
            type = linkAnnotation.type
        )
    }

    fun urlTrim(url: String): String {
        return if (url.endsWith("/")) removeTrailing(url) else url
    }

    private fun removeTrailing(s: String): String {
        return s.trimEnd('/')
    }

    fun setParameters(value: String?, parameters: Map<String, String>): String {
        return value?.replace(Regex("\\{\\s*(\\w+)\\}"), { parameters[it.groupValues[1]] ?: it.value }) ?: ""
    }

    fun getHash(value: String): String {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(value.toByteArray(StandardCharsets.UTF_8))
            return convertToHex(md.digest())
        } catch (e: NoSuchAlgorithmException) {
            return value
        }
    }

    private fun convertToHex(messageDigest: ByteArray): String {
        val bigint = BigInteger(1, messageDigest)
        var hexText = bigint.toString(16).padStart(32, '0')
        return hexText.toUpperCase()
    }

}
}

//companion object {
//    fun toIndentedString(o: Any?): String {
//        return if (o == null) "null" else o.toString().replace("\n", "\n    ")
//    }
//}
