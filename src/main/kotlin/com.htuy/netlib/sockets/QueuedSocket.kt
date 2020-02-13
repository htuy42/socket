package com.htuy.netlib.sockets

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.common.ObjectSerializer
import com.htuy.concurrent.CallbackQueue
import com.htuy.concurrent.timeout
import com.htuy.dependency.ModuleManager
import com.htuy.netlib.storage.NetstoredObject
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.io.IOException
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeoutException

class ListenerMap {

    val inner = HashMap<Class<out Serializable>, Any>()

    fun <T : Serializable> set(key: Class<out T>, listener: (T) -> Serializable?) {
        inner.put(key, listener)
    }

    fun <T : Serializable> get(key: Class<out T>): (T) -> Serializable? {
        return inner.get(key) as (T) -> Serializable
    }

    fun remove(key: Any) {
        inner.remove(key)
    }

    fun containsKey(key: Class<out Any>): Boolean {
        return key in inner
    }
}

/**
 * Socket that operates on a pair of queues (an out q and an in q).
 * In networked circumstances, this is a good adapter to simplify the local management part of the socket.
 * In non-networked circumstacess, this can serve as the entire solution, with two processes that wish to have a socket
 * sharing opposite ends of the same 2 queues.
 *
 * I just wasted like an hour faffing about with the types on this to get them to all line up. ughhhh
 */
internal class QueuedSocket(
    private val inQueue: CallbackQueue<Message<out Serializable>>,
    private val outQueue: CallbackQueue<Message<out Serializable>>,
    override val connectedTo: Address,
    private val timeout: Long = 10000L,
    private val moduleManager: ModuleManager? = null
) : Socket {

    private val preCloseHandles = HashSet<() -> Unit>()

    private var isOpen: Boolean = true

    override fun isOpen(): Boolean {
        return isOpen
    }


    private fun handleClosed() {
        synchronized(preCloseHandles) {
            for (elt in preCloseHandles) {
                elt()
            }
        }
        for (elt in onClose) {
            elt()
        }
    }

    private val onClose = HashSet<() -> Unit>()

    override fun registerOnClose(onClose: () -> Unit) {
        this.onClose.add(onClose)
    }

    // to apply to messages before handling them
    private val preprocessors = ListenerMap()

    // to handle specific sorts of message
    private val typedListeners = ListenerMap()

    // to handle specific *individual* messages
    private val uidListeners: MutableMap<UUID, (Message<*>) -> Unit> = HashMap()

    // messages that we didn't have a handler for when they came in. If they sit here for too long,
    // we complain about them.
    private val missedMessages: MutableSet<Message<*>> = HashSet()

    override fun close() {
        isOpen = false
        handleClosed()
    }

    override fun error(message: String) {
        outQueue.put(makeErrorMessage(message))
    }

    override fun <T : Serializable> unregisterTypeListener(clazz: Class<out T>) {
        typedListeners.remove(clazz)
    }

    override fun push(toSend: Serializable) {
        outQueue.put(Message(toSend))
    }

    override fun <T> get(request: Serializable): Promise<T, Exception> {
        Logging.getLogger().trace { "Sending get for $request" }
        val deferred = deferred<T, Exception>()
        val outMessage = IdMessage(request, false)
        val outId = outMessage.id
        Logging.getLogger().trace { "Out id for message is $outId" }
        val handle = {
            synchronized(deferred) {
                if (!deferred.promise.isDone()) {
                    deferred.reject(IOException("Other socket closed for some reason, request will not be fulfilled"))
                }
            }
        }
        synchronized(preCloseHandles) {
            if (!isOpen) {
                handle()
                return deferred.promise
            }
            preCloseHandles.add(handle)
            uidListeners[outId] = {
                Logging.getLogger().trace { "Got response for $outId" }
                synchronized(preCloseHandles) {
                    synchronized(deferred) {
                        if (!deferred.promise.isDone()) {
                            val internal = it.contents as T
                            deferred.resolve(internal)
                            preCloseHandles.remove(handle)
                        }
                    }
                }
            }
        }
        timeout(timeout) {
            synchronized(deferred) {
                if (!deferred.promise.isDone()) {
                    deferred.reject(TimeoutException("Timed out waiting for response"))
                }
            }
        }
        outQueue.put(outMessage)
        return deferred.promise
    }

    override fun <T : Serializable> registerTypeListener(clazz: Class<out T>, listener: (T) -> Serializable?) {
        synchronized(missedMessages) {
            checkMissedMessages(listener, clazz, outQueue)
            typedListeners.set(clazz, listener)
        }
    }

    override fun <T : Serializable> registerPreprocessor(clazz: Class<out T>, listener: (T) -> Serializable?) {
        synchronized(missedMessages) {
            checkMissedMessages(listener, clazz, inQueue)
            preprocessors.set(clazz, listener)
        }
    }

    override fun <T> getSynch(request: Serializable): T {
        return get<T>(request).get()
    }

    fun drainInQueue() {
        synchronized(inQueue) {
            while (!inQueue.isEmpty()) {
                val message = inQueue.poll() ?: continue
                synchronized(missedMessages) {
                    if (!consumeIfMatchable(message as Message<Serializable>)) {
                        missedMessages.add(message)
                    }
                }
            }
        }
    }

    init {
        inQueue.registerCallback(::drainInQueue)
    }

    private fun <T> checkMissedMessages(
        listener: (T) -> Serializable?,
        clazz: Class<out Any>,
        destination: CallbackQueue<Message<out Serializable>>
    ) {
        val iter = missedMessages.iterator()
        while (iter.hasNext()) {
            val message = iter.next()
            if (message.contents!!::class.java.canonicalName.equals(clazz.canonicalName)) {
                consumeMessage(listener, message as Message<T>, destination)
                iter.remove()
            }
        }
    }

    private fun <T> consumeMessage(
        listener: (T) -> Serializable?,
        message: Message<T>,
        destination: CallbackQueue<Message<out Serializable>>
    ) {
        Logging.getLogger().trace { "Consuming $message" }
        val res = listener(message.contents!!)
        if (res != null) {
            if (message is IdMessage) {
                val isReturning = if (destination == outQueue) {
                    true
                } else {
                    Logging.getLogger()
                        .error { "Message returning issue: look into this. Likely related to preprocessing, but make sure" }
                    message.returning
                }
                Logging.getLogger().trace { "Consumed as id ${message.id}, sending back as returning: $isReturning" }
                destination.put(IdMessage(res, isReturning, message.id, message.processedAs))
            } else {
                Logging.getLogger().trace { "Consumed as non-id message" }
                destination.put(Message(res, message.processedAs))
            }
        }
    }

    private fun <T:Serializable> consumeIfMatchable(message: Message<T>): Boolean {
        val realMessage = if (moduleManager != null && message.contents is NetstoredObject) {
            Message(
                ObjectSerializer.netstoredBytesToObject(
                    message.contents as NetstoredObject,
                    moduleManager
                ) as Serializable
            )
        } else {
            message
        }

        if (realMessage.isError) {
            throw Exception(realMessage.contents as String)
        } else if (realMessage is IdMessage && realMessage.returning) {
            Logging.getLogger().trace { "Got id message of id ${realMessage.id}" }
            if (realMessage.id in uidListeners) {
                uidListeners[realMessage.id]!!.invoke(realMessage)
                return true
            } else {
                Logging.getLogger().error { "Message id ${realMessage.id} wasn't present, what to do???" }
                error("Got a realMessage with an id we weren't expecting")
                return true
            }
        } else {
            if (message.contents != null) {
                val contents = message.contents!!
                // some odd issue with kotlin typing, it insists upon this just like the godfather insists upon itself
                val s = contents as Serializable
                if (preprocessors.containsKey(s::class.java) && s::class.java !in message.processedAs) {
                    message.processedAs.add(s::class.java)
                    consumeMessage(typedListeners.get(s::class.java), realMessage, inQueue)
                    return true
                } else if (typedListeners.containsKey(s::class.java)) {
                    consumeMessage(typedListeners.get(s::class.java), realMessage, outQueue)
                    return true
                } else {
                    return false
                }
            } else {
                error("Got a message with null contents")
                return true
            }
        }
    }
}

