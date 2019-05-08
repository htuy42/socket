package com.htuy.common



class JLogger(private val header: String, private val level: Int = 0) {
    fun debug(makeString: () -> String) {
        if (level >= Logging.DEBUG_LEVEL) {
            println("$header:debug @ ${makeString.invoke()}")
        }
    }

    fun info(makeString: () -> String) {
        if (level >= Logging.INFO_LEVEL) {
            println("$header:info @ ${makeString.invoke()}")
        }
    }

    fun error(makeString: () -> String) {
        if (level >= Logging.ERROR_LEVEL) {
            println("$header:error @ ${makeString.invoke()}")
        }
    }

    fun warn(makeString: () -> String) {
        if (level >= Logging.WARN_LEVEL) {
            println("$header:warn @ ${makeString.invoke()}")
        }
    }

    fun trace(makeString: () -> String) {
        if (level >= Logging.TRACE_LEVEL) {
            println("$header:trace @ ${makeString.invoke()}")
        }
    }
}

class Logging {
    companion object {

        val TRACE_LEVEL = 5
        val INFO_LEVEL = 4
        val DEBUG_LEVEL = 3
        val WARN_LEVEL = 2
        val ERROR_LEVEL = 1
        val loggerBox = OnetimeBox<JLogger>()
        fun getLogger(): JLogger {
            return loggerBox.getItem()
        }
    }
}