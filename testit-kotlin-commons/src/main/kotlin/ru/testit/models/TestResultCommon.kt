package ru.testit.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.testit.services.Utils

@Serializable
data class TestResultCommon(
    var uuid: String? = null,
    var externalId: String = "",
    var externalKey: String = "",
    var workItemIds: MutableList<String> = mutableListOf<String>(),
    var className: String = "",
    var spaceName: String = "",
    var labels: MutableList<Label> = mutableListOf<Label>(),
    var linkItems: MutableList<LinkItem> = mutableListOf<LinkItem>(),
    var resultLinks: MutableList<LinkItem> = mutableListOf<LinkItem>(),
    var attachments: MutableList<String> = mutableListOf<String>(),
    var name: String = "",
    var title: String = "",
    var message: String = "",
    var itemStatus: ItemStatus? = null,
    private var itemStage: ItemStage? = null,
    var description: String = "",
    private var steps: MutableList<StepResult> = mutableListOf<StepResult>(),
    var start: Long = 0L,
    var stop: Long = 0L,
    @Contextual var throwable: Throwable? = null,
    val parameters: MutableMap<String, String> = mutableMapOf<String, String>(),
    var automaticCreationTestCases: Boolean = false
): ResultWithSteps {

    fun setItemStage(stage: ItemStage) {
        this.itemStage = stage
    }

    override fun getSteps(): MutableList<StepResult> {
        return steps
    }

    fun setSteps(steps: MutableList<StepResult>): TestResultCommon {
        this.steps = steps
        return this
    }

    override fun toString(): String {
        return StringBuilder("class TestResult {\n")
            .append("    uuid: ").append(Utils.toIndentedString(this.uuid)).append("\n")
            .append("    externalId: ").append(Utils.toIndentedString(this.externalId)).append("\n")
            .append("    workItemIds:").append(Utils.toIndentedString(this.workItemIds)).append("\n")
            .append("    className:").append(Utils.toIndentedString(this.className)).append("\n")
            .append("    spaceName: ").append(Utils.toIndentedString(this.spaceName)).append("\n")
            .append("    labels: ").append(Utils.toIndentedString(this.labels)).append("\n")
            .append("    linkItems: ").append(Utils.toIndentedString(this.linkItems)).append("\n")
            .append("    resultLinks:").append(Utils.toIndentedString(this.resultLinks)).append("\n")
            .append("    attachments:").append(Utils.toIndentedString(this.attachments)).append("\n")
            .append("    name: ").append(Utils.toIndentedString(this.name)).append("\n")
            .append("    title: ").append(Utils.toIndentedString(this.title)).append("\n")
            .append("    message: ").append(Utils.toIndentedString(this.message)).append("\n")
            .append("    itemStatus: ").append(Utils.toIndentedString(this.itemStatus)).append("\n")
            .append("    itemStage: ").append(Utils.toIndentedString(this.itemStage)).append("\n")
            .append("    description:").append(Utils.toIndentedString(this.description)).append("\n")
            .append("    steps:").append(Utils.toIndentedString(this.steps)).append("\n")
            .append("    throwable: ").append(Utils.toIndentedString(this.throwable)).append("\n")
            .append("    start: ").append(Utils.toIndentedString(this.start)).append("\n")
            .append("    stop: ").append(Utils.toIndentedString(this.stop)).append("\n")
            .append("    parameters: ").append(Utils.toIndentedString(this.parameters)).append("\n")
            .append("    automaticCreationTestCases:").append(Utils.toIndentedString(this.automaticCreationTestCases)).append("\n")
            .append("}")
            .toString()
    }
}
