package com.htuy.common

import com.htuy.netlib.sockets.InternetSockets


open class Configurator{
    fun setupAddressBox(){
        Address.provider.initialize(InternetSockets())
    }

    fun setupLoggerBox(loggerHeader : String = ""){
        Logging.loggerBox.initialize(JLogger(loggerHeader))
    }

    fun run(loggerHeader : String = "", logLevel : Int = Logging.ERROR_LEVEL){
        setupAddressBox()
        setupLoggerBox(loggerHeader)
    }
}