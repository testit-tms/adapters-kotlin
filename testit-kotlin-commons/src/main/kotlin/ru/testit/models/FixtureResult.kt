package ru.testit.models

import kotlinx.serialization.Serializable

@Serializable
class FixtureResult : ResultWithSteps, ResultWithAttachments {
    var name: String? = null
    var itemStatus: ItemStatus? = null
    var itemStage: ItemStage? = null
    var description: String? = null
    private val steps = mutableListOf<StepResult>()
    val linkItems = mutableListOf<LinkItem>()
    private val attachments = mutableListOf<String>()
    var parent: String? = null
    var start: Long? = null
    var stop: Long? = null
    var parameters = mutableMapOf<String, String>()


    override fun getSteps():  MutableList<StepResult>  {
        return steps
    }

    override fun getAttachments(): MutableList<String>  {
        return attachments
    }
}