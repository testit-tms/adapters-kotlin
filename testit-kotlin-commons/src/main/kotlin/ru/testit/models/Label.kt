package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.services.Utils

@Serializable
class Label {
    private var name: String? = null

    fun getName(): String? {
        return this.name
    }

    fun setName(name: String): Label {
        this.name = name
        return this
    }

    override fun toString() =
        """
                class Label {
                    name: ${Utils.toIndentedString(this.name)}
                }

            """.trimIndent()
}