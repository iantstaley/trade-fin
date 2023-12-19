package com.example.client

import com.example.state.IOUState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
//import com.github.kittinunf.fuel.*
//import com.github.kittinunf.fuel.core.FuelManager

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

//fun main(args: Array<String>) {
   //h ExampleClientRPC().main(args)
//}

private class ExampleClientRPC {
    companion object {
        val logger: Logger = loggerFor<ExampleClientRPC>()
        private fun logState(state: StateAndRef<IOUState>) = logger.info("{}", state.state.data)

        private fun IRTransfer(state: StateAndRef<IOUState>) {

            val args = state.state.data

        //FuelManager.instance.basePath = "http://192.168.0.105:3000"
            //logger.info("working dir is" + System.getProperty("user.dir"))
            val cburl :String = ConfigReader();
            logger.info("Invoking bank transfer request to  " +cburl)
            //FuelManager.instance.basePath = cburl
       // "/transfers".httpPost().body(args.toString()).responseString { request, response, result ->
            //make a POST and do something with response
            //val (data, error) = result
           // if (error == null) {
                //do something when success
                //logger.info("{}", result)
           // } else {
                //error handling
            //}
        }
        }

    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ExampleClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.example.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all existing and future IOU states in the vault.
        val (snapshot, updates) = proxy.vaultTrack(IOUState::class.java)


        // Log the 'placed' IOU states and listen for new ones.
        //update.produced.forEach { logState(it) }
       // snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
           // update.produced.forEach { IRTransfer(it) }
        }
    }
//}
