package com.htuy.netlib.sockets

import com.htuy.common.Address
import nl.komponents.kovenant.Promise
import java.io.Serializable

typealias SocketCallback = (Serializable) -> Serializable?

typealias Listener<I> = (I) -> Unit


interface Socket

{

    /**
     * Send a message without any explicit expectation of a response
     */
    fun push(toSend: Serializable)

    /**
     * Send a message to the other end with the explicit expectation of a response. If a response is not received
     * by timeout, the promise will error.
     */
    fun <T> get(request: Serializable): Promise<T, Exception>

    /**
     * Send a message to the other end with the explicit expectation of a response, and do not return
     * until that response is received.
     */
    fun <T> getSynch(request: Serializable): T

    /**
     * Register a listener for a given type of message. This will handle all future pushes from the other end for
     * that type. It will also handle any recently missed messages (ones that have not yet timed out) of that type.
     */
    fun <T : Serializable> registerTypeListener(clazz: Class<out T>,listener: (T) -> Serializable?)

    fun <T : Serializable> unregisterTypeListener(clazz : Class<out T>)

    /**
     * Send an error message over the socket
     */
    fun error(message: String)

    /**
     * The address that lives at the other end of the socket
     */
    val connectedTo: Address


    /**
     * Register a preprocessor. This works in more or less the same way as a type listener, except that instead of
     * piping the response to the other end of the socket, it puts it back into our end.
     * This can be used to transform elements for other handlers to process, or to filter them. Note that if the identical
     * object is returned, the preprocessor will not get to see it again. It is also not possible to create cycles of preprocessors
     * 1,2,3 such that A -> 1 -> B, B-> 2 -> C, C -> 3 -> A, either:  a preprocessor only gets to see a message once, ever.
     *
     */
    fun <T : Serializable> registerPreprocessor(clazz : Class<out T>, listener : (T) -> Serializable?)

    fun close()

    fun registerOnClose(onClose : () -> Unit)

    fun isOpen() : Boolean

    fun <T> tryGetSynch(request : Serializable, handler : (() -> T?)) : T?{
        return try{
            getSynch<T>(request)
        } catch(e : Throwable){
            e.printStackTrace()
            handler()
        }
    }

    fun tryPush(request : Serializable, handler : () -> Unit){
        try{
            push(request)
        } catch(e : Throwable){
            e.printStackTrace()
            handler()
        }
    }

}