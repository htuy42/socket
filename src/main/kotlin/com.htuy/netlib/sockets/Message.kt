package com.htuy.netlib.sockets

import java.io.Serializable
import java.util.*

/**
 * Internal data types. Technically sent across com.htuy.sockets, but not exposed to recipients.
 * Note that you never need to manually wrap anything in these. This is done by the underlying socket itself for utility.
 * You can send whatever serializable thing you want over the sockets.
 */
internal open class Message<T>(open val contents: T, processedAs : HashSet<Class<*>>? = null, val isError: Boolean = false) : Serializable{
    @Transient
    // for tracking the things we have already preprocessed a message as, to avoid processing loops
    val processedAs = processedAs?:HashSet()
}

internal class IdMessage<T>(override val contents: T,
                              val returning: Boolean,
                              val id: UUID = UUID.randomUUID(),
                              processedAs: HashSet<Class<*>>? = null) : Message<T>(contents,processedAs)

internal fun makeErrorMessage(contents : String) : Message<String>{
    return Message(contents,isError = true)
}

