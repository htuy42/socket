package com.htuy.concurrent

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking


// map. The difference here is that we will evaluate all of the transforms concurrently to the extent possible.
// return order will still be maintained as expected. This has non zero overhead, so it only makes sense where
// transform is blocking / expensive, ie if it is an io call
inline fun <T, R> Iterable<T>.splitMap(crossinline transform: (T) -> R): List<R> {
    val obj = this
    return runBlocking {
        var ind = 0
        val allLaunches = ArrayList<Job>()
        val res = ArrayList<R?>()
        for (x in 0 until obj.count()) {
            res.add(x, null)
        }
        for (elt in obj.iterator()) {
            val pos = ind
            ind++
            allLaunches.add(launch {
                res.set(pos, transform(elt))
            })
        }
        allLaunches.forEach {
            it.join()
        }
        return@runBlocking res.map {
            it!!
        }
    }
}

// for each. The difference here is that we will evaluate all of the runs concurrently.
// return order will still be maintained as expected. This will not necessarily maintain ordering:
// each call will be started in the order of the list, but they may finish in different orders.
inline fun <T> Iterable<T>.splitForEach(crossinline run: (T) -> Unit) {
    val obj = this
    runBlocking {
        val allLaunches = ArrayList<Job>()
        for (elt in obj.iterator()) {
            allLaunches.add(launch {
                run(elt)
            })
        }
        allLaunches.forEach { it.join() }
    }
}