//package com.htuy.netlib.sockets
//
//import com.htuy.common.Address
//import io.reactivex.Flowable
//import java.io.Serializable
//
///**
// * I never wound up using protocols. It's definitely a decent idea, in terms of cleaning up the interfaces everything
// * exposes and reducing the amount of mental tracking one needs to do, but I just never felt like doing it.
// *
// * With that said, this implementation may or may not make any actual sense.
// */
//interface Protocol {
//    fun getListeners(connectedTo : Socket): Map<Class<out Any>, (Serializable) -> Serializable?> = mapOf()
//    fun getPreprocessors(connectedTo : Socket): Map<Class<out Any>, (Serializable) -> Serializable?> = mapOf()
//    fun init(connectedTo: Socket){}
//}
//
//class ProtocolBuilder : SocketProvider {
//    private lateinit var sockets: SocketProvider
//    private val protocols = ArrayList<Protocol>()
//
//    fun withInternetSockets(): ProtocolBuilder {
//        if(::sockets.isInitialized){
//            throw IllegalStateException("Do not change the underlying socket provider after already setting it!")
//        }
//        sockets = InternetSockets()
//        return this
//    }
//
//    // only really for local testing.
//    internal fun withLocalSockets(socketsToUse: LocalSocketNet): ProtocolBuilder {
//        if(::sockets.isInitialized){
//            throw IllegalStateException("Do not change the underlying socket provider after already setting it!")
//        }
//        sockets = socketsToUse
//        return this
//    }
//
//    fun with(protocol: Protocol): ProtocolBuilder {
//        protocols.add(protocol)
//        return this
//    }
//
//    override fun listenOn(addr: Address, callback: Listener<Socket>) {
//        if(!::sockets.isInitialized){
//            throw IllegalStateException("Initialize sockets first with withInternetSockets()")
//        }
//        sockets.listenOn(addr) {
//            for (elt in protocols) {
//                elt.init(it)
//                for (listener in elt.getPreprocessors(it)) {
//                    it.registerPreprocessor(listener.key, listener.value)
//                }
//                for (listener in elt.getListeners(it)) {
//                    it.registerTypeListener(listener.key, listener.value)
//                }
//            }
//            callback(it)
//        }
//    }
//
//    override fun getSocketTo(to: Address, from: Address): Socket {
//        if(!::sockets.isInitialized){
//            throw IllegalStateException("Initialize sockets first with withInternetSockets()")
//        }
//        val socket = sockets.getSocketTo(to, from)
//        for (elt in protocols) {
//            elt.init(socket)
//            for (listener in elt.getPreprocessors(socket)) {
//                socket.registerPreprocessor(listener.key, listener.value)
//            }
//            for (listener in elt.getListeners(socket)) {
//                socket.registerTypeListener(listener.key, listener.value)
//            }
//        }
//        return socket
//    }
//}