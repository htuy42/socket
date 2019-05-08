package com.htuy.netlib.storage

import com.htuy.common.ObjectSerializer
import java.io.Serializable

/**
 * Another thing we currently don't use. The point here is that it allows us to update things without restarting the
 * network: objects that need this applied to them wrap themselves in a pointer to where we can get the jar,
 * and then we liveload the jar into our path. This is kind of unsafe, but safety is something we don't actively
 * concern ourself with. The reason its not in use right now is just because all the things I've built so far
 * haven't needed to stay alive for long periods of time, so it hasn't been relevant. But its also probably not
 * super stable since I've never used it
 */

data class NetstoredObject(val dependencies: List<String>, val internal: ByteArray) : Serializable {
    constructor(dependencies: List<String>, internal: Serializable) : this(dependencies,
                                                                           ObjectSerializer.objectToBytes(internal))

    fun getRepresentedObject(): Serializable {
        return ObjectSerializer.bytesToObject(internal) as Serializable
    }
}

interface Netstorable : Serializable{
    val dependencies: List<String>
    fun toNetstored() : NetstoredObject{
        return NetstoredObject(dependencies, ObjectSerializer.objectToBytes(this))
    }
}

