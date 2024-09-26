package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.services.Utils

@Serializable
class ClassContainer {
    var uuid: String? = null
    var name: String? = null
    val beforeEachTest: MutableList<FixtureResult> = mutableListOf()
    val afterEachTest: MutableList<FixtureResult> = mutableListOf()
    val beforeClassMethods: MutableList<FixtureResult> = mutableListOf()
    val afterClassMethods: MutableList<FixtureResult> = mutableListOf()
    val children: MutableList<String> = mutableListOf()
    var start: Long? = null
    var stop: Long? = null




    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("class ClassContainer {\n")
        sb.append("    uuid: ").append(Utils.toIndentedString(this.uuid)).append("\n")
        sb.append("    name: ").append(Utils.toIndentedString(this.name)).append("\n")
        sb.append("    beforeEachTest:").append(Utils.toIndentedString(this.beforeEachTest)).append("\n")
        sb.append("    afterEachTest:").append(Utils.toIndentedString(this.afterEachTest)).append("\n")
        sb.append("    beforeClassMethods:").append(Utils.toIndentedString(this.beforeClassMethods)).append("\n")
        sb.append("    afterClassMethods:").append(Utils.toIndentedString(this.afterClassMethods)).append("\n")
        sb.append("    children: ").append(Utils.toIndentedString(this.children)).append("\n")
        sb.append("    start: ").append(Utils.toIndentedString(this.start)).append("\n")
        sb.append("    stop: ").append(Utils.toIndentedString(this.stop)).append("\n")
        sb.append("}")
        return sb.toString()
    }
}