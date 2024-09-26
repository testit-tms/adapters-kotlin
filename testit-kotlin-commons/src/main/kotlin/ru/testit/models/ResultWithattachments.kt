package ru.testit.models

/**
 * The marker interface for model objects with attachments.
 */
interface ResultWithAttachments {
    /**
     * Gets attachments.
     *
     * @return the attachments
     */
    fun getAttachments(): List<String>
}