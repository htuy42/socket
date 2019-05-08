package com.htuy.dependency

import com.htuy.constant.LOCAL_MODULE_INSTALL_LOCATION
import java.io.File
import java.net.URL
import java.net.URLClassLoader

// repository is the path to the folder modules will be loaded from and stored in. If this is only being used for
// serving modules (ie getModule), the loader can be left null, but this will case loadModule to except
// repository should end with a /
class ModuleManager(private val repository: String = LOCAL_MODULE_INSTALL_LOCATION, private val loader: ModuleLoader? = null) {
    companion object {
        // we need to be able to call addURL on the URL classloader, so here is a horrible hack to accomplish that.
        private val classLoader = ClassLoader.getSystemClassLoader() as URLClassLoader
        private val addUrlExposedMethod = classLoader::class.java.superclass.getDeclaredMethod("addURL",
                                                                                               URL::class.java)

        init {
            addUrlExposedMethod.isAccessible = true
        }
    }

    private val loadedModules = HashSet<String>()

    // Ensure that a given module is available / if its classes are instantiated they will function properly
    fun loadModule(toLoad: String) {
        if(loader == null){
            throw IllegalStateException("Can't load a module if we don't have a module loader!")
        }
        if (toLoad in loadedModules) {
            return
        }
        val moduleFile = getModuleFile(toLoad)
        if (!moduleFile.exists()) {
            moduleFile.createNewFile()
            moduleFile.writeBytes(loader.getModuleBytes(toLoad))
        }
        loadModuleFromJar(moduleFile)
        loadedModules.add(toLoad)
    }

    // Get the byte contents of a given module. Used generally to then send the module somewhere else
    fun getModule(toLoad: String): ByteArray {
        val moduleFile = getModuleFile(toLoad)
        if (!moduleFile.exists()) {
            throw IllegalArgumentException("We do not have that module, so we can't send it to you!")
        }
        return moduleFile.readBytes()
    }

    private fun getModuleFile(moduleName: String): File {
        return File("$repository$moduleName.jar")
    }

    private fun loadModuleFromJar(moduleFile: File) {
        addUrlExposedMethod.invoke(classLoader, moduleFile.toURI().toURL())
    }

}