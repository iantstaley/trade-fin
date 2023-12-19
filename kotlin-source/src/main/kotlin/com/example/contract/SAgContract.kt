package com.example.contract
import com.example.state.SAState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.contextLogger

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [SAState], which in turn encapsulates an [SA].
 *
 * For a new [SA] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [SA].
 * - An Create() command with the public keys of both the buyer and the seller.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class SAgContract : Contract {

    private val logger = contextLogger()
    companion object {
        @JvmStatic
        val SA_CONTRACT_ID = "com.example.contract.SAgContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
 override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create ->
            requireThat {
                // Generic constraints around the IOU transaction.
                "No inputs should be consumed when issuing an SA." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val out = tx.outputsOfType<SAState>().single()
                "The seller and the buyer cannot be the same entity." using (out.buyer != out.seller)
                //"All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                // SA-specific constraints.
                "The SA's value must be non-negative." using (out.saleamount > 0)
            }
            is Commands.Accept ->
                    requireThat {
                        // SA-specific constraints.
                        "SAVerification process has one input state." using (tx.inputs.size == 1)
                        "Only one output state should be created." using (tx.outputs.size == 1)
                        val input = tx.inputsOfType<SAState>().single()
                        "The SA's value must be non-negative." using (input.saleamount > 0)
                        "Ensure that iput SA's state is Created" using (input.state == "Created")
                    }
            is Commands.Notarise ->
                    requireThat {

                        "Only one output state should be created." using (tx.outputs.size == 1)
                        val input = tx.inputsOfType<SAState>().single()
                      " Ensure that The SA state should be Verified (by the seller)" using (input.state =="Verified")
                        logger.info ("Input state data is in contract is " + input.state)

                    }
        }
  }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create: Commands
        class Accept :Commands
        class Notarise: Commands

    }
}
