package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.services.Utils

@Serializable
data class Label (
    var name: String? = null
){

    override fun toString() =
        """
                class Label {
                    name: ${Utils.toIndentedString(this.name)}
                }

            """.trimIndent()
}