package com.htuy.common

/**
 * A container to hold a value. It can only be written a single time. Essentially used to represent a var that
 * may be initialized but then never written again. A bit messy and not that useful to be honest.
 */
class OnetimeBox<T>{

    // our object
    private var internal : T? = null

    // whether we've been initialized
    private var initialized : Boolean = false

    /**
     * Get the contained item. Will error if initialize has not been called on this box yet
     */
    fun getItem() : T{
        if(internal == null){
            throw IllegalAccessException("Can't get gotten until initialized with initialize!")
        } else{
            return internal!!
        }
    }

    /**
     * Store the value the box should hold. Will error if this is ever called more than once on the same box
     */
    fun initialize(item : T){
        if(initialized){
            throw IllegalAccessException("Can only initialize once!")
        } else {
            initialized = true
            internal = item
        }
    }

    fun isReady() : Boolean{
        return internal != null
    }

}