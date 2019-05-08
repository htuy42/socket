package com.htuy.netlib.sockets

import com.htuy.common.Address
import java.io.Serializable

/**
 * Provides com.htuy.sockets between processes, as well as allowing them to register themselves as providers.
 * In a networked case, this would most likely be listening on ports and the establishing com.htuy.sockets for incoming
 * connections, where for a local case it can just hold a map of registrants and connect them on request.
 */
interface SocketProvider{
    /**
     * Get a socket between ourself and the given address. Must also include our own address in the request
     */
    fun getSocketTo(to : Address, from : Address) : Socket

    /**
     * Register to receive connections at the given address.
     */
    fun listenOn(self : Address,  callback : Listener<Socket>)

}