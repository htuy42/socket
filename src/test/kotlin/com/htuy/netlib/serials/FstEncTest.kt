package com.htuy.netlib.serials

import com.htuy.common.Address
import com.htuy.common.Configurator
import com.htuy.netlib.sockets.InternetSockets
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import nl.komponents.kovenant.deferred
import org.junit.Assert.*
import org.junit.Test
import java.io.Serializable

data class TreeThing(val subs : List<TreeThing>, val value : Double) : Serializable

fun makeTreeThing(d : Int, w : Int) : TreeThing{
    if(d == 0){
        return TreeThing(listOf(),0.0)
    }
    val subs = (0 until w).map{
        makeTreeThing(d - 1,w)
    }
    return TreeThing(subs,d.toDouble())
}

class FstEncTest{

    @Test
    fun benchmarkEncDec(){
        Configurator().run()
        val sockets1 = InternetSockets()
        val sockets2 = InternetSockets(encoderFactory = {FstEnc()}, decoderFactory = {FstDec()})
        var lstStart = 0L
        var timeSum1 = 0L
        var timeSum2 = 0L
        var currentThing : TreeThing? = null
        val a1 = Address.anyPortLocal()
        val a2 = Address.anyPortLocal()
        var done1 = deferred<Boolean,Exception>()
        var done2 = deferred<Boolean,Exception>()
        sockets1.listenOn(a1){
            it.registerTypeListener(TreeThing::class.java){
                timeSum1 += System.currentTimeMillis() - lstStart
                assert(it == currentThing)
                done1.resolve(true)
                null
            }
        }
        sockets2.listenOn(a2){
            it.registerTypeListener(TreeThing::class.java){
                timeSum2 += System.currentTimeMillis() - lstStart
                assert(it == currentThing)
                done2.resolve(true)
                null
            }
        }
        val outbound1 = sockets1.getSocketTo(a1,Address.anyPortLocal())
        val outbound2 = sockets2.getSocketTo(a2,Address.anyPortLocal())
        var totalTimes = 0
        for(w in 0 until 9){
            for(d in 0 until 9){
                totalTimes++
                currentThing= makeTreeThing(w,d)
                lstStart = System.currentTimeMillis()
                outbound1.push(currentThing)
                done1.promise.get()
                lstStart = System.currentTimeMillis()
                outbound2.push(currentThing)
                done2.promise.get()
                done1 = deferred<Boolean,Exception>()
                done2 = deferred<Boolean,Exception>()
            }
            println("at $w done, time for standard is $timeSum1 and time for new is $timeSum2")
        }
    }

}