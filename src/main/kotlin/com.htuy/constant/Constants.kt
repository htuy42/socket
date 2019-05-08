package com.htuy.constant

import com.htuy.common.SystemUtilies

/**
 * Just exists to help keep track of global constant values. The main thing this is used for is finding directories
 * where things are kept, at least for the moment.
 */

// where we should try to get modules from with the dependency manager
val REMOTE_MODULE_HOST_LOCATION = "jpattiz.com/modules/"

val BASE_DIR_WINDOWS = "d:\\dev\\kt\\rundir"

val home = System.getProperty("user.home")

//val BASE_DIR_LINUX = "$home/kt/rundir"

// currently hard coded. This way I can place things in my own rundir and distribute just a king shell to people
// and they can use them. This is a temporary solution, though.
val BASE_DIR_LINUX = "/home/jpattiz/kt/rundir"

// where we should install modules to with dependency manager
val LOCAL_MODULE_INSTALL_LOCATION = pathInBaseDir("installedModules",true)

fun pathInBaseDir(basePath : String, endWithSeparator: Boolean = false, linuxOverride: Boolean = false) : String{
    return pathInBaseDir(listOf(basePath),endWithSeparator,linuxOverride)
}

fun pathInBaseDir(basePath : List<String>, endWithSeparator : Boolean = false, linuxOverride : Boolean = false) : String{
    val doWindows = SystemUtilies.isWindows() && !linuxOverride
    val separator = if(doWindows){"\\"}else{"/"}
    if(basePath.isEmpty()){
        return if(!doWindows){
            "$BASE_DIR_LINUX/${if(endWithSeparator){separator} else {""}}"
        } else {
            "$BASE_DIR_WINDOWS${if(endWithSeparator){separator} else {""}}"
        }
    }
    val path = basePath.joinTo(StringBuilder(),separator).toString()
    return if(!doWindows){
        "$BASE_DIR_LINUX/$path${if(endWithSeparator){separator} else {""}}"
    } else {
        "$BASE_DIR_WINDOWS\\$path${if(endWithSeparator){separator} else {""}}"
    }
}

fun pathToLocalModule(moduleName : String) : String{
    return "$LOCAL_MODULE_INSTALL_LOCATION$moduleName.jar"
}
