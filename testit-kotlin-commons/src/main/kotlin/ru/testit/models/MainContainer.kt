package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.services.Utils

@Serializable
data class MainContainer(
    var uuid: String? = null,
    var beforeMethods: MutableList<FixtureResult> = mutableListOf(),
    var afterMethods: MutableList<FixtureResult> = mutableListOf(),
    var children: MutableList<String> = mutableListOf(),
    var start: Long? = null,
    var stop: Long? = null
) {

    override fun toString(): String {
        return buildString {
            append("class MainContainer {\n")
            append("    uuid: ").append(Utils.toIndentedString(this@MainContainer.uuid)).append("\n")
            append("    beforeMethods:").append(Utils.toIndentedString(this@MainContainer.beforeMethods)).append("\n")
            append("    afterMethods:").append(Utils.toIndentedString(this@MainContainer.afterMethods)).append("\n")
            append("    children:").append(Utils.toIndentedString(this@MainContainer.children)).append("\n")
            append("    start:").append(Utils.toIndentedString(this@MainContainer.start)).append("\n")
            append("    stop: ").append(Utils.toIndentedString(this@MainContainer.stop)).append("\n")
            append("}")
        }
    }
}