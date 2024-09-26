package ru.testit.models

import kotlinx.serialization.Serializable
import ru.testit.services.Utils
//import kotlinx.serialization.ContextualSerialization

/**
 * Model describes step.
 */
@Serializable
data class StepResult(
    var name: String? = null,
    var itemStatus: ItemStatus? = null,
    var itemStage: ItemStage? = null,
    var description: String? = null,
    private var steps: MutableList<StepResult> = mutableListOf(),
    var linkItems: MutableList<LinkItem> = mutableListOf(),
    private var attachments: MutableList<String> = mutableListOf(),
//    TODO: find fix
//    var throwable: Throwable = null,

    var start: Long? = null,
    var stop: Long? = null,
    var parameters: Map<String, String> = mapOf()
) : ResultWithSteps, ResultWithAttachments {

    override fun getAttachments(): MutableList<String> {
        return attachments
    }


    override fun getSteps(): MutableList<StepResult> {
        return steps
    }


    fun setSteps(steps: MutableList<StepResult>): StepResult {
        this.steps = steps
        return this
    }

    override fun toString(): String {
        return buildString {
            append("class StepResult {\n")
            append("    name: ").append(Utils.toIndentedString(this@StepResult.name)).append("\n")
            append("    itemStatus:").append(Utils.toIndentedString(this@StepResult.itemStatus)).append("\n")
            append("    itemStage:").append(Utils.toIndentedString(this@StepResult.itemStage)).append("\n")
            append("    description:").append(Utils.toIndentedString(this@StepResult.description)).append("\n")
            append("    steps: ").append(Utils.toIndentedString(this@StepResult.steps)).append("\n")
            append("    linkItems:").append(Utils.toIndentedString(this@StepResult.linkItems)).append("\n")
            append("    attachments:").append(Utils.toIndentedString(this@StepResult.attachments)).append("\n")
//            append("    throwable:").append(Utils.toIndentedString(this@StepResult.throwable)).append("\n")
            append("    start: ").append(Utils.toIndentedString(this@StepResult.start)).append("\n")
            append("    stop: ").append(Utils.toIndentedString(this@StepResult.stop)).append("\n")
            append("    parameters:").append(Utils.toIndentedString(this@StepResult.parameters)).append("\n")
            append("}")
        }
    }
}