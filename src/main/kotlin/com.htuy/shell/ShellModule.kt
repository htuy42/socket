package com.htuy.shell

interface ShellModule{
    fun getCommands() : List<ShellCommand>
    val submodules : Map<String,ShellModule>
    val name : String
}