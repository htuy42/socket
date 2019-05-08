package com.htuy.common

import com.google.gson.Gson
import com.htuy.dependency.ModuleManager
import com.htuy.netlib.storage.NetstoredObject
import java.io.*
import java.util.*
import org.nustaq.serialization.FSTConfiguration
import org.reflections.Reflections

annotation class RegisteredSerial

annotation class AbstractSerial


/**
 * Serializes stuff. Just shorthands because otherwise I have to look up the syntax every time.
 * The only time this does anything significant is when it deals with netstored objects, but we don't currently
 * use those.
 */
class ObjectSerializer{
    companion object {
        private val gson : Gson by lazy{Gson()}
        private val conf : FSTConfiguration by lazy{ FSTConfiguration.createDefaultConfiguration()}
        fun objectToString(toSerialize: Serializable): String {
            return String(Base64.getEncoder().encode(objectToBytes(toSerialize)))
        }
        fun stringToObject(string: String): Any {
            return bytesToObject(Base64.getDecoder().decode(string.toByteArray()))
        }
        fun objectToBytes(toSerialize: Serializable) : ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(toSerialize)
            objectOutputStream.close()
            return byteArrayOutputStream.toByteArray()
        }

        fun bytesToObject(bytes : ByteArray, manager : ModuleManager? = null) : Any{
            val objectInputStream = ObjectInputStream(ByteArrayInputStream(bytes))
            val obj = objectInputStream.readObject()
            if(manager != null && obj is NetstoredObject){
                for(elt in obj.dependencies){
                    manager.loadModule(elt)
                }
                return bytesToObject(obj.internal)
            }
            return obj
        }

        fun netstoredBytesToObject(netstored: NetstoredObject, manager: ModuleManager) : Any{
            for(elt in netstored.dependencies){
                manager.loadModule(elt)
            }
            return bytesToObject(netstored.internal)
        }

        fun objectToNetstoredBytes(toStore : Serializable, dependencies: List<String>) : ByteArray{
            return objectToBytes(NetstoredObject(dependencies, objectToBytes(toStore)))
        }

        fun anyToJson(obj : Any) : String{
            return gson.toJson(obj)
        }
        fun <T> fromJson(json : String, clazz : Class<T>) : T{
            return gson.fromJson(json,clazz)
        }

        fun fstObjToBytes(obj : Any) : ByteArray{
            return conf.asByteArray(obj)
        }

        fun fstObjectFromBytes(bytes : ByteArray) : Any{
            return conf.asObject(bytes)
        }

        fun registerClassToSerialize(clazz : Class<*>){
            conf.registerClass(clazz)
        }

        fun registerAllTaggedClasses(print : Boolean = false){
            val reflections = Reflections("com.htuy")
            val annotated = reflections.getTypesAnnotatedWith(RegisteredSerial::class.java)
            for(elt in annotated){
                registerClassToSerialize(elt)
                if(print){
                    println("Registered ${elt.name}")
                }
            }
            val toGetSubs = reflections.getTypesAnnotatedWith(AbstractSerial::class.java)
            for(elt in toGetSubs){
                registerClassToSerialize(elt)
                if(print){
                    println("Registered ${elt.name}")
                }
                val subs = reflections.getSubTypesOf(elt)
                for(sub in subs){
                    registerClassToSerialize(sub)
                }
                if(print){
                    println("Registered ${elt.name}")
                }
            }
        }

    }
}


