package com.htuy.common

import com.htuy.netlib.sockets.Socket
import com.htuy.netlib.sockets.SocketProvider
import java.io.IOException
import java.io.Serializable
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private var lastUsedPort = 45555

/**
 * Utilities for working with addresses
 */
data class Address(val address: String, val port: Int) : Serializable {


    // our socket to this addrss
    private var sock: Socket? = null

    companion object {

        // the provider to use for socketTo and tryGetSynch. Note that this must be initialized before those methods will work
        val provider: OnetimeBox<SocketProvider> = OnetimeBox()

        private val socketCache = ConcurrentHashMap<Address,Socket>()

        val localHost : String by lazy{ Inet4Address.getLocalHost().hostAddress}

        /**
         * Create an address representing the given port on the local machine
         */
        fun withPortLocal(port: Int): Address {
            return Address(Inet4Address.getLocalHost().hostAddress, port)
        }

        // This is super specific, but also useful in my case. It gets the local network
        // ip address for my machine on the brown network, giving me the ip other machines can connect
        // to
        fun brownAddress(): String? {
            val nets = NetworkInterface.getNetworkInterfaces()
            for (netint in Collections.list(nets)) {
                if(netint.inetAddresses.hasMoreElements()){
                    val host = netint.inetAddresses.nextElement().hostName
                    if(host.substring(0,5) == "10.38"){
                        return host
                    }
                }
            }
            return null
        }

        /**
         * Create an address from an inet socket address
         */
        fun fromInetSocketAddress(inet: InetSocketAddress): Address {
            return Address(inet.hostString, inet.port)
        }

        /**
         * Any local address is acceptable: pick one "at random." In practice, picks the next unbound port it can find,
         * so these will tend to be sequential. This may also attempt to produce an address out of bounds if it is
         * called too often
         */
        fun anyPortLocal(): Address {
            while (!isLocalPortFree(lastUsedPort)) {
                lastUsedPort++
            }
            if (lastUsedPort > 65535) {
                throw IllegalStateException("all ports are used.")
            }
            return Address(Inet4Address.getLocalHost().hostAddress, lastUsedPort++)
        }

        /**
         * Determine whether or not a given local port is available, to try to avoid using ports other applications
         * are using when we randomly get ports
         */
        private fun isLocalPortFree(port: Int): Boolean {
            try {
                ServerSocket(port).close()
                return true
            } catch (e: IOException) {
                return false
            }
        }
    }

    /**
     * Convert to an InetSocketAddress representing the same thing
     */
    fun toInet(): InetSocketAddress {
        return InetSocketAddress(address, port)
    }

    /**
     * Get a socket to this address. Uses the same socket over and over: doesn't make a new one unless the old one
     * is closed
     */
    fun socketTo(): Socket? {
        checkSocketExists()
        return sock
    }

    /**
     * Sockets tryGetSynch, except that this attempts to create a socket to use, and if it fails to do so due
     * to some sort of connect error that will also trigger handler
     */
    fun tryGetSynch(request: Serializable, handler: () -> Serializable?): Serializable? {
        if (!checkSocketExists()) {
            Logging.getLogger().error{"Couldn't send $request. Running handler"}
            return handler()
        }
        return sock?.tryGetSynch(request, handler) ?: handler()
    }


    private fun checkSocketExists(): Boolean {
        Logging.getLogger().debug{"Checking if socket exists"}
        if (!(sock?.isOpen() ?: false)) {
            Logging.getLogger().warn{"Didnt have a socket to $address : $port. Trying to make one"}
            try {
                sock = socketCache.getOrPut(this){
                    provider.getItem().getSocketTo(this,Address.anyPortLocal())
                }
                Logging.getLogger().warn{"Successfully made a socket to $address : $port"}
            } catch (e: Exception) {
                Logging.getLogger().error{"Couldn't get a socket to $address : $port"}
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    fun tryPush(request: Serializable, handler: () -> Unit) {
        if (!checkSocketExists()) {
            handler()
            return
        }
        sock?.push(request) ?: handler()

    }
}

