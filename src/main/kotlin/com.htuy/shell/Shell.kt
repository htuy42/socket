package com.htuy.shell

import jdk.nashorn.tools.Shell

/**
 * I wrote this after writing all the king stuff, and everything just being syncrhonous made it feel so nice and easy
 */
class KShell {
    private val registeredCommands = HashMap<String, ShellCommand>()
    private val mappings = HashMap<String, String>()
    internal val availableModules = HashMap<String, ShellModule>()

    init {
        registerCommand(HandleEqualityCommand(mappings))
        registerCommand(ListCommand(registeredCommands))
        registerCommand(ModuleAddCommand(this))
    }

    suspend fun run() {
        var line: String? = readLine()
        while (line != null) {
            val cmds = preprocess(line)
            if (cmds.size == 0) {
                continue
            }
            val cmd = registeredCommands[cmds[0]]

            if (cmd != null) {
                if (cmd.argListSize != -1) {
                    if (cmds.size - 1 != cmd.argListSize) {
                        println("Invalid number of arguments to ${cmd.describe()}")
                    }
                }
                try {
                    println(cmd.execute(cmds.drop(1)))
                } catch (e: Exception) {
                    println("Problem executing $line")
                    e.printStackTrace()
                }
            } else {
                println("Couldn't parse $line into a command. Try again, or call ls to get a list of commands you can use")
            }
            line = readLine()
        }
    }

    private fun tryBindName(name : String, command : ShellCommand) : Boolean{
        if(registeredCommands[name] != null){
            return false
        } else {
            registeredCommands[name] = command
        }
        return true
    }

    fun registerCommand(command: ShellCommand) {
        var failed = ""
        val toUse = listOf(command.name, *((command.aliases).toTypedArray()))
        toUse.forEach{
            if(!tryBindName(it,command)){
                failed += "$it "
            }
        }
        if(failed != ""){
            println("Tried to add command, but $failed clashed with other already registered names. If there are more aliases, some of them may be bound.")
        }
    }

    fun registerModule(moduleName : String, module : ShellModule) : String{
        var foundCommands = ""
        for(elt in module.getCommands()){
            registerCommand(elt)
            foundCommands += "${elt.name} "
        }
        for(elt in module.submodules){
            availableModules[elt.key] = elt.value
        }
        return "from $moduleName added $foundCommands"
    }

    private fun preprocess(line: String): List<String> {
        val split = line.split(" ")
        val substituted = split.map {
            if (it in mappings) {
                mappings[it]!!
            } else {
                it
            }
        }
        return substituted
    }
}

private class ModuleAddCommand(val shell : KShell) : ShellCommand(){
    override val name: String = "module_add"
    override val aliases: List<String> = listOf("madd")
    override val description: String = "takes a name of a module class. Attempts to find it, instantiate it, and add it to the shell"
    override suspend fun execute(input: List<String>): String {
        val moduleName = input[0]
        if(moduleName in shell.availableModules){
            val module = shell.availableModules[moduleName]!!
            val res = shell.registerModule(moduleName,module)
            shell.availableModules.remove(moduleName)
            return res
        }
        // note that for our convenience we assume the classname starts with com.htuy.shellmodules. To use this with something else you'd need to change this
        lateinit var clazz : Class<*>
        try {
            clazz = java.lang.Class.forName("com.htuy.shellmodules.$moduleName")
        } catch (e: ClassNotFoundException) {
            clazz = java.lang.Class.forName("com.htuy.shellmodules.${moduleName}Kt")
        }
        val shellModule = clazz.constructors[0].newInstance() as ShellModule
        return shell.registerModule(moduleName, shellModule)
    }
}

private class ListCommand(val commands : Map<String,ShellCommand>) : ShellCommand(){
    override val name: String = "list"
    override val aliases: List<String> = listOf("ls")
    override val description: String = "list all existing commands and their descriptions"

    override suspend fun execute(input: List<String>): String {
        return commands.entries.map{
            if(it.value.name == it.key){
                it.value.describe()
            } else {
                null
            }
        }.filterNotNull().joinToString(separator = "\n\n")
    }

}

private class HandleEqualityCommand(val mappings : HashMap<String,String>) : ShellCommand() {
    override val name: String = "="
    override val description: String = "map [1] to [2], such that any future commands will have [1] replaced with [2]"
    override val argListSize: Int = 2
    override suspend fun execute(input: List<String>): String {
        mappings[input[0]] = input[1]
        return "mapped ${input[0]} to ${input[1]}"
    }

}