package ru.testit.models


data class TestItContext (
    var uuid: String? = null,
    var externalId: String? = null,
    var links: MutableList<LinkItem>? = null,
    var workItemIds: MutableList<String>? = null,
    var attachments: MutableList<String>? = null,
    var name: String? = null,
    var title: String? = null,
    var message: String? = null,
    var itemStatus: ItemStatus? = null,
    var description: String? = null,
    var parameters: MutableMap<String, String>? = null,
    var labels: MutableList<Label>? = null,
)