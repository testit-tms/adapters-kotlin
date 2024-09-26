package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.annotations.Description
import ru.testit.services.Utils

@Serializable
class LinkItem {
    var title: String? = null
    var url: String? = null
    var description: String? = null
    var type: LinkType? = null

    fun setTitle(title: String?): LinkItem {
        this.title = title
        return this
    }

    fun setDescription(description: String?): LinkItem {
        this.description = description
        return this
    }

    fun setType(type: LinkType?): LinkItem {
        this.type = type
        return this
    }

    fun setUrl(url: String?): LinkItem {
        this.url = url
        return this
    }



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