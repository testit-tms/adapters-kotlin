package ru.testit.services

import ru.testit.models.*
import java.util.Objects
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


// TODO: redundant java.util  Optional and Objects
/**
 * Storage for test results
 */
class ResultStorage {
    private val storage = ConcurrentHashMap<String, Any>()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    fun getTestResult(uuid: String): Optional<TestResultCommon> {
        return get(uuid, TestResultCommon::class.java)
    }

    fun getFixture(uuid: String): Optional<FixtureResult> {
        return get(uuid, FixtureResult::class.java)
    }

    fun getStep(uuid: String): Optional<StepResult> {
        return get(uuid, StepResult::class.java)
    }

    fun getTestsContainer(uuid: String): Optional<MainContainer> {
        return get(uuid, MainContainer::class.java)
    }

    fun getClassContainer(uuid: String): Optional<ClassContainer> {
        return get(uuid, ClassContainer::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    fun getList(uuid: String): Optional<MutableList<String>> {
        lock.readLock().lock()
        try {
            Objects.requireNonNull(uuid) { "Can't get result from storage: uuid can't be null" }
            return (storage[uuid] as? MutableList<String>).let { Optional.ofNullable(it) }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getStorage(): ConcurrentHashMap<String, Any> {
        return storage
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> get(uuid: String, clazz: Class<T>): Optional<T> {
        lock.readLock().lock()
        try {
            Objects.requireNonNull(uuid) { "Can't get result from storage: uuid can't be null" }
            return (storage[uuid] as? T).let { Optional.ofNullable(it) }
        } finally {
            lock.readLock().unlock()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> put(uuid: String, item: T): T {
        lock.writeLock().lock()
        try {
            Objects.requireNonNull(uuid) { "Can't put result to storage: uuid can't be null" }
            Objects.requireNonNull(item) { "Can't put result to storage: item can't be null" }
            storage[uuid] = item
            return item
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun remove(uuid: String) {
        lock.writeLock().lock()
        try {
            Objects.requireNonNull(uuid) { "Can't remove item from storage: uuid can't be null" }
            storage.remove(uuid)
        } finally {
            lock.writeLock().unlock()
        }
    }
}