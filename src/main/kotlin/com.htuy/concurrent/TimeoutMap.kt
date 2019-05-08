package com.htuy.concurrent

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A map whose items are removed if they are not re-put within (timeout) time. When an item is removed, handler() is called
 */

class TimeoutMap<K, V>(
    val timeout: Long,
    private val inner: ConcurrentHashMap<K, V> = ConcurrentHashMap(),
    private val handler: ((K, V) -> Unit)? = null
) : MutableMap<K, V> by inner {

    private val lock: ReentrantLock = ReentrantLock()

    data class QueueItem<K>(val key: K, val timeout: Long)

    private val timeouts = ConcurrentHashMap<K, Long>()
    // Using this queue allows us to avoid starting a coroutine to timeout each individual item in the map,
    // which would be the other obvious approach afaict
    private val timeoutQueue = PriorityQueue<QueueItem<K>>(10, object : Comparator<QueueItem<K>> {
        override fun compare(o1: QueueItem<K>, o2: QueueItem<K>): Int {
            return o1.timeout.compareTo(o2.timeout)
        }
    })

    override fun put(key: K, item: V): V? {
        var res: V? = null
        lock.withLock {
            res = inner.put(key, item)
            timeouts[key] = System.currentTimeMillis() + timeout
            timeoutQueue.add(QueueItem(key, System.currentTimeMillis() + timeout))
        }
        return res
    }

    init {
        launch {
            delay(timeout)
            while (true) {
                if (timeoutQueue.size == 0) {
                    delay(timeout)
                    continue
                }
                var top = timeoutQueue.peek()
                lock.withLock {
                    if (top.timeout < System.currentTimeMillis()) {
                        top = timeoutQueue.poll()
                        val thisTimeout = timeouts[top.key]
                        if (thisTimeout == null || thisTimeout < System.currentTimeMillis()) {
                            timeouts.remove(top.key)
                            val item = inner[top.key]
                            if (item != null) {
                                inner.remove(top.key)
                                handler?.invoke(top.key, item)
                            }
                        }
                    }
                }
                if (top.timeout > System.currentTimeMillis()) {
                    delay(top.timeout - System.currentTimeMillis())
                }
            }
        }
    }
}