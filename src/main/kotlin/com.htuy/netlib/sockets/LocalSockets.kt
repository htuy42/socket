package com.htuy.netlib.sockets

import com.htuy.common.Address
import com.htuy.concurrent.CallbackQueue
import java.io.Serializable
import java.util.*

/**
 * Socket provider that only runs locally, and requires all "processes" using it to live within the same process
 * (ie they could be separate co-routines or threads, but not separate processes). Intended for testing.
 */
class LocalSocketNet : SocketProvider {
    private val members: MutableMap<Address, Listener<Socket>> = HashMap()

    override fun listenOn(self: Address,  callback: Listener<Socket>) {
        members[self] = callback
    }

    override fun getSocketTo(to: Address, from: Address): Socket {
        return if (to in members) {
            val inToLocal: CallbackQueue<Message<out Serializable>> = CallbackQueue()
            val inToOther: CallbackQueue<Message<out Serializable>> = CallbackQueue()
            val local = QueuedSocket(inToLocal, inToOther, to)
            val other = QueuedSocket(inToOther, inToLocal, from)
            members[to]!!.invoke(other)
            local
        } else {
            throw IllegalArgumentException("No such address registered")
        }
    }
}

