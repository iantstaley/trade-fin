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
 * Created by mkshibu on 31/08/18..
 */
object LCCreationFlow {
    private val logger = contextLogger()

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val lcno: String, val lcState: String) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Letter of Credit.")
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
            //query saleAgreement by external saleagreementno
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(externalId = listOf(lcno))
            val inputStateAndRef = serviceHub.vaultService.queryBy(LCState::class.java, queryCriteria).states.single()
            logger.info("The result {}", inputStateAndRef)
            // Stage 3. Create the new LC state reflecting a new state of the letter of Credit.
            val inState = inputStateAndRef.state.data
            val outputLC = inState.withNewStatus(lcState)


            if(inState.sano != outputLC.sano)
            {
                //
                logger.info("Sale Agreement number mismatch in LC ",inState.sano, outputLC.sano)
                throw IllegalArgumentException("Sale Agreement number mismatch btw input LC and outPut LC")
            }

            // Stage 4. Create the Create command.
            //val signers = (inState.participants).map { it.owningKey }
            val signers = inState.advisingbank.owningKey
            val createCommand = Command(LCContract.Commands.Create(), signers)

            // Stage 5. Get a reference to a transaction builder.
            // Note: ongoing work to support multiple notary identities is still in progress.
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val builder = TransactionBuilder(notary = notary)

            // Stage 6. Create the transaction which comprises one input, one output and one command.
            builder.withItems(inputStateAndRef,
                    StateAndContract(outputLC, LCContract.LC_CONTRACT_ID),
                    createCommand)
            val counterParty =  inState.advisingbank
            // Stage 7. Verify and sign the transaction.
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder)

            val sessions = initiateFlow(counterParty!!)

            val stx = subFlow(CollectSignaturesFlow(ptx, listOf(sessions)))

            return subFlow(FinalityFlow(stx))
        }
    }

    @InitiatedBy(LCCreationFlow.Initiator::class)
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