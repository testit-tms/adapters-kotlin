package ru.testit.listener

import org.slf4j.LoggerFactory
import java.util.ServiceLoader

object ServiceLoaderListener {
    private val logger = LoggerFactory.getLogger(javaClass)

//    @Suppress("UNCHECKED_CAST")
    fun <T> load(type: Class<T>, classLoader: ClassLoader): List<T> {
        val loaded = mutableListOf<T>()
        val iterator = ServiceLoader.load(type, classLoader).iterator()
        while (nextSafely(iterator)) {
            try {
                val next = iterator.next() as T
                loaded.add(next)
                logger.debug("Found type {}", type)
            } catch (e: Exception) {
                logger.error("Could not load listener {}: {}", type, e)
            }
        }
        return loaded
    }

    private fun nextSafely(iterator: Iterator<*>): Boolean {
        try {
            return iterator.hasNext()
        } catch (e: Exception) {
            logger.error("nextSafely failed", e)
            return false
        }
    }
}