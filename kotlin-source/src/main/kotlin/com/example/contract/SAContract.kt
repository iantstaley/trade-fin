package com.example.contract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import com.example.state.SAState
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

open class SAContract : Contract {
    companion object {
        @JvmStatic
        val SA_CONTRACT_ID = "com.example.contract.SAContract"
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
                    //"Input state data is in contract is " + input.state

                }
        }

    }

    /**
     * This contract only implements one command, Create.
     */

    interface Commands : CommandData {
        class Create : Commands
        class Accept : Commands
        class Notarise : Commands
    }

}