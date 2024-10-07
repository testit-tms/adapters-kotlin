package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.annotations.Description
import ru.testit.services.Utils

@Serializable
data class LinkItem (
    var title: String,
    var url: String,
    var description: String,
    var type: LinkType
) {

    override fun toString() =
        """
                class LinkItem {
                    title: ${Utils.toIndentedString(this.title)}
                    url: ${Utils.toIndentedString(this.url)}
                    description: ${Utils.toIndentedString(this.description)}
                    type: ${Utils.toIndentedString(this.type)}
                }

            """.trimIndent()
}