package ru.testit.models

import org.slf4j.LoggerFactory


data class TestItContext (
    var uuid: String? = null,
    var externalId: String? = null,
    var links: MutableList<LinkItem>? = null,
    var workItemId: String? = null,
    var workItemIds: MutableList<String>? = null,
    var attachments: MutableList<String>? = null,
    var name: String? = null,
    var title: String? = null,
    var message: String? = null,
    var itemStatus: ItemStatus? = null,
    var description: String? = null,
    var parameters: MutableMap<String, String>? = null,
    var labels: MutableList<Label>? = null,
    var tags: MutableList<String>? = null,
    var layer: String? = null,
) {
    fun resolvedWorkItemIds(): MutableList<String>? {
        if (workItemIds != null) {
            LOGGER.warn("workItemIds is deprecated. Use workItemId with a single globalId instead.")
        }
        if (workItemId != null) {
            return mutableListOf(workItemId!!)
        }
        return workItemIds
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestItContext::class.java)
    }
}
