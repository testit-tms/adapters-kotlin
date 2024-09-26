package ru.testit.services

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.LinkedList
import java.util.Objects
import java.util.Optional

/**
 * Thread local context that stores information about not finished tests and steps.
 */
class Context : InheritableThreadLocal<LinkedList<String>>() {

    override fun initialValue(): LinkedList<String> {
        return LinkedList()
    }
    override fun childValue(parentStepContext: LinkedList<String>): LinkedList<String> {
        return LinkedList(parentStepContext)
    }
}

@Serializable
data class ThreadContext(
    @Contextual
    private val context: Context = Context()
) {

    /**
     * Returns last (most recent) uuid.
     *
     * @return last uuid.
     */
    fun getCurrent(): Optional<String> {
        val uuids = context.get()
        return if (uuids.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(uuids.first)
        }
    }

    /**
     * Returns first (oldest) uuid.
     *
     * @return first uuid.
     */
    fun getRoot(): Optional<String> {
        val uuids = context.get()
        return if (uuids.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(uuids.last)
        }
    }

    /**
     * Adds new uuid.
     *
     * @param uuid the value
     */
    fun start(uuid: String) {
        Objects.requireNonNull(uuid, "step uuid")
        context.get().addFirst(uuid)
    }

    /**
     * Removes latest added uuid. Ignores empty context.
     *
     * @return removed uuid.
     */
    fun stop(): Optional<String> {
        val uuids = context.get()
        return if (uuids.isNotEmpty()) {
            Optional.of(uuids.removeFirst())
        } else {
            Optional.empty()
        }
    }

    /**
     * Removes all the data stored for current thread.
     */
    fun clear() {
        context.remove()
    }

}