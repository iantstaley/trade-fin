package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.LCContract
import com.example.state.LCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger

/**
 * Created by mkshibu on 31/08/18.
 */
object LCVerificationFlow {
    private val logger = contextLogger()

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val lcno: String, val lcState: String) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Sale Agreement.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()


        @Suspendable
        override fun call(): SignedTransaction {

            logger.info("Generating transaction  ")
            //Query vault
            //query the LC by external lc number
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(externalId = listOf(lcno))
            val inputStateAndRef = serviceHub.vaultService.queryBy(LCState::class.java, queryCriteria).states.single()
            logger.info("The result {}", inputStateAndRef)
            // Stage 3. Create the new LC state reflecting a new state of the saleAgreement.
            val inState = inputStateAndRef.state.data
            val outputLC = inState.withNewStatus(lcState)



            // Stage 4. Create the transfer command.
           // val signers = (inState.participants).map { it.owningKey }
            val signers = inState.seller.owningKey
            val verifyCommand = Command(LCContract.Commands.Verify(), signers)

            // Stage 5. Get a reference to a transaction builder.

            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val builder = TransactionBuilder(notary = notary)

            // Stage 6. Create the transaction which comprises one input, one output and one command.
            builder.withItems(inputStateAndRef,
                    StateAndContract(outputLC, LCContract.LC_CONTRACT_ID),
                    verifyCommand)
            val counterParty =  inState.seller
            // Stage 7. Verify and sign the transaction.
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder)

            val sessions = initiateFlow(counterParty!!)
            val stx = subFlow(CollectSignaturesFlow(ptx, listOf(sessions)))

            return subFlow(FinalityFlow(stx))
        }
    }

    @InitiatedBy(LCVerificationFlow.Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}