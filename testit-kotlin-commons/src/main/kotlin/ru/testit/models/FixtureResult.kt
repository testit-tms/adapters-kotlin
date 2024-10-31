package ru.testit.models

import kotlinx.serialization.Serializable

@Serializable
data class FixtureResult  (
    var name: String? = null,
    var itemStatus: ItemStatus? = null,
    var itemStage: ItemStage? = null,
    var description: String? = null,
    private val steps: MutableList<StepResult> = mutableListOf(),
    val linkItems: MutableList<LinkItem> = mutableListOf(),
    private val attachments: MutableList<String> = mutableListOf(),
    var parent: String? = null,
    var start: Long? = null,
    var stop: Long? = null,
    var parameters: MutableMap<String, String> = mutableMapOf()
): ResultWithSteps, ResultWithAttachments {



    override fun getSteps():  MutableList<StepResult>  {
        return steps
    }

    override fun getAttachments(): MutableList<String>  {
        return attachments
    }
}