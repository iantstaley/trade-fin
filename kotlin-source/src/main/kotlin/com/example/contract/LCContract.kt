package com.example.contract


import com.example.state.LCState
import com.example.state.SAState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

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
open class LCContract : Contract {
    companion object {
        @JvmStatic
        val LC_CONTRACT_ID = "com.example.contract.LCContract"
    }


    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
         when (command.value) {
            is Commands.Request ->
                requireThat {
                    // Generic constraints around the LC transaction.
                  val out = tx.outputsOfType<LCState>().single()

                  "Only one output state should be created." using (tx.outputs.size == 1)

                   "The issuing bank  and the verifying bank cannot be the same entity." using (out.issuingbank != out.advisingbank)
                    //"All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                    // LC-specific constraints.
                    "The LC's value must be non-negative." using (out.lcmount > 0)
                }
            is Commands.Create ->
            {}

      /*      requireThat{
                 val input = tx.inputsOfType<LCState>().single()
                 val out = tx.outputsOfType<LCState>().single()
                 "LC creator should not tamper with sale sale agreement no mentioned in the LC" using (input.sano == out.sano)

            }*/
            is Commands.Verify ->

            requireThat{
                val input = tx.inputsOfType<LCState>().single()
                val out = tx.outputsOfType<LCState>().single()
                "LC Verifying should not tamper with sale sale agreement no mentioned in the LC" using (input.sano == out.sano)
                "The issuing bank  and the verifying bank cannot be the same entity." using (out.issuingbank != out.advisingbank)

            }
            is Commands.Accept ->
           requireThat {

               val input = tx.inputsOfType<LCState>().single()
               val out = tx.outputsOfType<LCState>().single()
               "LC accepting party should not tamper with sale sale agreement no mentioned in the LC" using (input.sano == out.sano)
               //Seller has to ensure that LC is Verified by his bank.
               "input LC state is verified by the advising bank " using (input.state == "Verified")

           }
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Request(): Commands
        class Create: Commands
        class Verify: Commands
        class Accept: Commands

    }
}
