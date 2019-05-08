package com.htuy.common

import java.util.*


// a class that has a unique id and associates all equality with it. Basically the reason to do this is
// because pointer equality / relationships are less durable under serialization, esp the equality
abstract class Idd{
    open val id : UUID = UUID.randomUUID()
    override fun equals(other: Any?): Boolean {
        if(other is Idd){
            return other.id == id
        }
        return false
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
}