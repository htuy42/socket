package com.htuy.machines

import com.htuy.common.SystemUtilies


class MachineList(val machines : List<Machine>)

data class Machine(val name : String, val groups : List<String>, val properties : Map<String,String>, var load : Int = 0) : Comparable<Machine>{
    override fun compareTo(other: Machine): Int {
        return load.compareTo(other.load)
    }

    // note this only works from the lovely old department machines. This line is basically the thing that means
    // this and swarm only actually work at Brown
    fun sendSshCommand(command : String, silenced : Boolean = false){
        SystemUtilies.shellCommand("ssh $name $command",silenced = silenced)
    }

}