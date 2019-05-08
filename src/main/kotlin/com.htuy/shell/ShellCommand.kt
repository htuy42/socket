package com.htuy.shell

abstract class ShellCommand{
    abstract val name : String
    open val aliases : List<String> = listOf()
    abstract val description : String
    open val argListSize : Int = -1
    abstract suspend fun execute(input : List<String>) : String

    fun describe() : String{
        return "$name: $description. Aliases: ${aliases.joinToString(" ")}"
    }
}