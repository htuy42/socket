package com.htuy.concurrent

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A queue that offers callbacks when an item is added.
 */
class CallbackQueue<T>(val q: LinkedBlockingQueue<T> = LinkedBlockingQueue()) : BlockingQueue<T> by q {
    private val callbacks: MutableList<() -> Unit> = ArrayList()
    private fun runCallbacks() {
        callbacks.forEach { it() }
    }

    override fun put(e: T) {
        synchronized(this) {
            q.put(e)
            runCallbacks()
        }
    }

    override fun add(element: T): Boolean {
        synchronized(this) {
            val res = q.add(element)
            if (res) {
                runCallbacks()
            }
            return res
        }
    }

    override fun offer(element: T): Boolean {
        synchronized(this) {
            val res = q.offer(element)
            if (res) {
                runCallbacks()
            }
            return res
        }
    }

    override fun offer(element: T, timeout: Long, unit: TimeUnit?): Boolean{
        synchronized(this) {
            val res = q.offer(element, timeout, unit)
            if (res) {
                runCallbacks()
            }
            return res
        }
    }

    /**
     * add a callback. This will be fired every time an item is added. It wil also be called
     * once immediately if the q is not currently empty.
     */
    fun registerCallback(callback: () -> Unit) {
        synchronized(this) {
            callbacks.add(callback)
            if (!q.isEmpty()) {
                callback()
            }
        }
    }
}