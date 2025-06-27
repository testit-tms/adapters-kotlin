package ru.testit.utils

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

/**
 * HTML escaping utilities to prevent XSS attacks
 * Functions for escaping HTML tags to prevent XSS attacks
 */
object HtmlEscapeUtils {
    private const val NO_ESCAPE_HTML_ENV_VAR = "NO_ESCAPE_HTML"
    
    // Regex pattern to detect HTML tags that are not already escaped
    private val htmlTagPattern = Regex("<\\S.*?(?:>|/>)")
    
    // Regex patterns to escape only non-escaped characters
    private val lessThanPattern = Regex("(?<!\\\\)<")
    private val greaterThanPattern = Regex("(?<!\\\\)>")

    /**
     * Escapes HTML tags to prevent XSS attacks.
     * First checks if the string contains HTML tags using regex pattern.
     * Only performs escaping if HTML tags are detected.
     * Escapes all < as \< and > as \> only if they are not already escaped.
     * Uses regex with negative lookbehind to avoid double escaping.
     * 
     * @param text The text to escape
     * @return Escaped text or original text if no HTML tags found or null if input is null
     */
    fun escapeHtmlTags(text: String?): String? {
        if (text == null) {
            return null
        }

        // First check if the string contains HTML tags
        if (!htmlTagPattern.containsMatchIn(text)) {
            return text // No HTML tags found, return original string
        }

        // Use regex with negative lookbehind to escape only non-escaped characters
        var result = lessThanPattern.replace(text, "\\\\<")
        result = greaterThanPattern.replace(result, "\\\\>")

        return result
    }

    /**
     * Escapes HTML tags in all String properties of an object using reflection
     * Also processes List properties: if List of objects - calls escapeHtmlInObjectList,
     * if List of Strings - escapes each string
     * Can be disabled by setting NO_ESCAPE_HTML environment variable to "true"
     * 
     * @param obj The object to process
     * @return The processed object with escaped strings
     */
    fun <T : Any> escapeHtmlInObject(obj: T?): T? {
        if (obj == null) {
            return null
        }

        // Check if escaping is disabled via environment variable
        val noEscapeHtml = System.getenv(NO_ESCAPE_HTML_ENV_VAR)
        if ("true".equals(noEscapeHtml, ignoreCase = true)) {
            return obj
        }

        try {
            processProperties(obj)
        } catch (e: Exception) {
            // Silently ignore reflection errors
        }

        return obj
    }

    /**
     * Escapes HTML tags in all String properties of objects in a list using reflection
     * Can be disabled by setting NO_ESCAPE_HTML environment variable to "true"
     * 
     * @param list The list of objects to process
     * @return The processed list with escaped strings in all objects
     */
    fun <T : Any> escapeHtmlInObjectList(list: MutableList<T>?): MutableList<T>? {
        if (list == null) {
            return null
        }

        // Check if escaping is disabled via environment variable
        val noEscapeHtml = System.getenv(NO_ESCAPE_HTML_ENV_VAR)
        if ("true".equals(noEscapeHtml, ignoreCase = true)) {
            return list
        }

        for (obj in list) {
            escapeHtmlInObject(obj)
        }

        return list
    }

    @Suppress("UNCHECKED_CAST")
    private fun processProperties(obj: Any) {
        val kClass = obj::class
        val properties = kClass.memberProperties

        for (property in properties) {
            try {
                // Only process mutable properties
                if (property !is KMutableProperty1<*, *>) {
                    continue
                }
                
                val mutableProperty = property as KMutableProperty1<Any, Any?>
                mutableProperty.isAccessible = true
                
                val value = mutableProperty.get(obj)

                when {
                    value is String -> {
                        // Escape String properties
                        mutableProperty.set(obj, escapeHtmlTags(value))
                    }
                    value is MutableList<*> && value.isNotEmpty() -> {
                        processList(value as MutableList<Any>)
                    }
                    value != null && !isSimpleType(value::class) -> {
                        // Process nested objects (but not simple types like Int, LocalDateTime, etc.)
                        escapeHtmlInObject(value)
                    }
                }
            } catch (e: Exception) {
                // Silently ignore reflection errors for individual properties
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processList(list: MutableList<Any>) {
        if (list.isEmpty()) {
            return
        }

        val firstElement = list[0]

        when (firstElement) {
            is String -> {
                // List of Strings - escape each string
                for (i in list.indices) {
                    val element = list[i]
                    if (element is String) {
                        list[i] = escapeHtmlTags(element) ?: element
                    }
                }
            }
            else -> {
                // List of objects - process each object
                for (item in list) {
                    escapeHtmlInObject(item)
                }
            }
        }
    }

    /**
     * Checks if a type is a simple type that doesn't need HTML escaping
     * 
     * @param kClass Type to check
     * @return True if it's a simple type
     */
    private fun isSimpleType(kClass: KClass<*>): Boolean {
        return when (kClass) {
            Boolean::class, Byte::class, Char::class, Short::class, Int::class, Long::class,
            Float::class, Double::class, String::class -> true
            else -> {
                // Check for common Java/Kotlin types
                val className = kClass.qualifiedName
                className?.startsWith("java.time.") == true ||
                className?.startsWith("java.util.UUID") == true ||
                className?.startsWith("java.net.") == true ||
                kClass.isSubclassOf(Enum::class) ||
                kClass.java.isPrimitive
            }
        }
    }
} 