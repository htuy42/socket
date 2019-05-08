package com.htuy.machines

import com.htuy.common.ObjectSerializer
import com.htuy.constant.pathInBaseDir
import java.io.File
import java.util.*



// if multiple copies are made from the same collection file, they will obviously maintain separate loads values.
// This is also not at all disk persistent.
class MachineCollection(clusterFile : String = pathInBaseDir(listOf("ssh","sshtargets.json"))){
    companion object {
        val LOW_LOAD = 1
        val MEDIUM_LOAD = 2
        val HIGH_LOAD = 4
        // this isn't actually integer.maxvalue. This is big enough that no amount of low,medium,high loads will fill it.
        // the reason we don't use max is twofold: a machine holding a low load and an infinite shouldn't overflow,
        // and after everyone has @ least 1 infinite load we still want to be able to compare and start assigning
        // second infinite loads
        val INFINITE_LOAD = 1000000
    }
    private val clusterFile = File(clusterFile)
    private var machineList = readInCluster().machines

    private val loads : Queue<Machine> = PriorityQueue(machineList)


    // might give the same machine more than once, if even after adding load it is still the least utilized. If this isn't
    // desirable, write more code lmao
    fun getMachinesLoadManaged(load : Int, numberOfMachines : Int) : List<Machine>{
        // we don't change load since they are all doing the same amount of more work, so its not relevant.
        // load is only a relative value, its actual numerical value doesn't mean anything
        if(numberOfMachines == machineList.size){
            // we can return the actual object because its only exposed as a list,  not a mutable one. Technically they can
            // coerce it to a mutable list but I'm willing to let people do that and mess themselves up if they really want to lol
            return machineList
        }
        if(numberOfMachines > machineList.size){
            throw IllegalArgumentException("There are not that many machines. We asked for $numberOfMachines machines, and there are ${machineList.size}")
        }
        val res = ArrayList<Machine>()
        while(res.size < numberOfMachines){
            val new = loads.poll()
            res.add(new)
            new.load += load
            loads.add(new)
        }
        return res
    }

    private fun readInCluster() : MachineList{
        if(!clusterFile.exists()){
            throw IllegalArgumentException("File passed in for cluster file does not exist. Got ${clusterFile.absolutePath}")
        }
        return ObjectSerializer.fromJson(clusterFile.readText(),MachineList::class.java)
    }
}