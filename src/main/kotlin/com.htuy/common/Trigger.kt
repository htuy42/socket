package com.htuy.common

import java.util.concurrent.locks.ReentrantLock

// not concurrenct safe
class CountTrigger(private val countTo: Int, private val trigger: () -> Unit) {
    var count: Int = 0
    fun count1() {
        count++
        if (count >= countTo) {
            trigger()
            count = 0
        }
    }
}
class ThreadSafeCountTrigger(private val countTo: Int, private val trigger: () -> Unit) {
    private var count: Int = 0

    fun count1() {
        synchronized(count) {
            count++
            if (count >= countTo) {
                trigger()
                count = 0
            }
        }
    }
}

// like with count trigger, if you want to use it with a bunch of threads you need to use ThreadSafeTimeTrigger
class TimeTrigger(private val frequency : Long, private val trigger: () -> Unit){
    private var nextTime = System.currentTimeMillis() + frequency
    fun check(){
        val time = System.currentTimeMillis()
        if(time > nextTime){
            trigger()
            nextTime = System.currentTimeMillis() + frequency
        }
    }
}

class ThreadSafeTimeTrigger(private val frequency : Long, private val trigger: () -> Unit){
    private var nextTime = System.currentTimeMillis() + frequency
    private val lock = ReentrantLock()
    fun check(){
        // if someone else is already doing it right now, just let them deal with it and return. This means we don't have
        // perfect precision, but this is already imprecise and its assumed that precision isn't important here.
        // If we need better precision, we should use an inverse mechanism, where the thing waits a given amount of time
        // and then just runs. This is useful when we only want to perform this task at certain points in time,
        // rather than whenever the timeout happens to expire (ie recomputing a configuration based on results of the last runs)
        if(lock.tryLock()) {
            val time = System.currentTimeMillis()
            synchronized(this) {
                if (time > nextTime) {
                    trigger()
                    nextTime = System.currentTimeMillis() + frequency
                }
            }
            lock.unlock()
        }
    }
}