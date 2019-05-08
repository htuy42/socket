package com.htuy.dependency

import com.htuy.common.Address
import com.htuy.netlib.sockets.InternetSockets
import java.io.Serializable
import java.net.URL

interface ModuleLoader {
    fun getModuleBytes(moduleName: String): ByteArray
}

// loads modules from an internet hosted repository. RepositoryBase should be a url ending in / like:
// jpattiz.com/repository/
// where it is possible to append module names to refer to a given module. Ie jpattiz.com/repository/<fancyLibrary>.jar
// should point to fancyLibrary's jar file
class InternetModuleLoader(private val repositoryBase: String) : ModuleLoader {
    override fun getModuleBytes(moduleName: String): ByteArray {
        return URL("${repositoryBase}$moduleName.jar").readBytes()
    }
}

data class ModuleRequest(val moduleName: String) : Serializable

data class ModuleResponse(val module: ByteArray?) : Serializable

// loads modules from another machine running the ModuleService protocol.
class RemoteHostModuleLoader(private val remoteHost: Address) : ModuleLoader {
    companion object {
        fun serveModules(manager: ModuleManager) {
            val addr = Address.anyPortLocal()
            InternetSockets().listenOn(addr) {
                it.registerTypeListener(ModuleRequest::class.java) {
                    it as ModuleRequest
                    var result: ModuleResponse = ModuleResponse(null)
                    try {
                        result = ModuleResponse(manager.getModule(it.moduleName))
                    } catch (e: IllegalArgumentException) {
                        // pass. If its thrown we just want to return null
                    }
                    result
                }
            }
        }
    }

    private val socket = InternetSockets().getSocketTo(remoteHost,
                                                       Address.anyPortLocal())

    override fun getModuleBytes(moduleName: String): ByteArray {
        val result = socket.get<ModuleResponse>(ModuleRequest(moduleName)).get().module
        return result ?: throw IllegalStateException("Remote host didn't have the module $moduleName. Cannot continue!")
    }
}