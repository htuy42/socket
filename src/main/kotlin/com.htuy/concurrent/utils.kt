package com.htuy.concurrent

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

/**
 * Execute a function after a fixed amount of time (in ms)
 */
fun timeout(time: Long, execute: () -> Unit) {
    launch {
        delay(time)
        execute.invoke()
    }
}