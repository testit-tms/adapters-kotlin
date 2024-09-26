package ru.testit
import ru.testit.kotlin.client.models.LinkModel
import ru.testit.kotlin.client.models.LinkType


fun main() {
    println(LinkModel("", true, null, "title", "desc", LinkType.Repository))

}