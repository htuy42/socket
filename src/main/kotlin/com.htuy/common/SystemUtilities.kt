package com.htuy.common

import com.htuy.constant.pathInBaseDir
import sun.plugin2.util.SystemUtil
import java.io.File
import java.util.concurrent.TimeUnit

class SystemUtilies {
    companion object {
        // get the name of the jar file this class can be loaded from (and is being loaded from currently)
        fun getJarName(clazz: Class<out Any>): File {
            return File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI())
        }

        fun isWindows(): Boolean {
            return System.getProperty("os.name").startsWith("Windows")
        }

        private val runningProcesses = ArrayList<Process>()

        private var killed = false

        fun killAll() {
            synchronized(killed) {
                killed = true
            }
            for (elt in runningProcesses) {
                elt.destroyForcibly()
            }
        }

        fun waitForSystemExit(){
            // a simple spell, but quite unbreakable
            Thread.currentThread().join()
        }

        fun shellCommand(str: String, workingDir: File = File(pathInBaseDir("")), silenced: Boolean = false) {
            if (silenced) {
                synchronized(killed) {
                    if (killed) {
                        return
                    }
                    runningProcesses.add(
                        ProcessBuilder(*str.split(" ").toTypedArray())
                            .directory(workingDir)
                            .redirectOutput(File("/dev/null"))
                            .redirectError(File("/dev/null"))
                            .start()
                    )
                }
            } else {
                ProcessBuilder(*str.split(" ").toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
            }
        }
    }

}

