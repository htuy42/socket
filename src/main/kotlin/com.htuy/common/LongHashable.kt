package com.htuy.common

import kotlin.math.abs

/**
 * Able to be hashed to a long instead of an int
 */
interface LongHashable{

    /**
     * Get a consistent hash value that is a long
     */
    fun longHash() : Long

    /**
     * Get a consistent hash value that is a positive long
     */
    fun unsignedLongHash() : Long{
        return abs(longHash())
    }

}