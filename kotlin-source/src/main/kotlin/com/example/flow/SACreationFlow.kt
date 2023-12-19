package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.SAContract
import com.example.state.SAState
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
 * Created by mkshibu on 13/03/18.
 */
object SACreationFlow {
    private val logger = contextLogger()
    @InitiatingFlow
    @StartableByRPC
    class Initiator( val saState : SAState
            /*    val saleamount: Int,
                   val seller: Party,
                   val buyer: Party,
                   val state:String,
                   val saleagreementno:String,
                   val itemofsale:String,
                   val sellercountry:String,
                   val buyercountry:String,
                   val agreementDate:String,
                   val quantityofsale:String,
                   val notarisedby:String,
                   val countryofnotary:String,
                    */



    ) : FlowLogic<SignedTransaction>() {
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

            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            progressTracker.currentStep = SACreationFlow.Initiator.Companion.GENERATING_TRANSACTION
            logger.info("Generating transaction  ")

            val txCommand = Command(SAContract.Commands.Create(), saState.participants.map { it.owningKey })
            logger.info("Transaction builder   ")
            val txBuilder = TransactionBuilder(notary)
                  // .addOutputState(saState, SAContract.SA_CONTRACT_ID )
                  // .addCommand(txCommand)
            txBuilder.withItems(StateAndContract(saState,SAContract.SA_CONTRACT_ID),txCommand)

            txBuilder.verify(serviceHub)
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
            val otherPartyFlow = initiateFlow(saState.seller!!)
            logger.info("initiating flow   ")
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(otherPartyFlow)))
            return subFlow(FinalityFlow(fullySignedTx ))

        }
    }

    @InitiatedBy(SACreationFlow.Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an SA transaction." using (output is SAState)
                    val sa = output as SAState
                    "I won't accept Sale Agreement with a Value over 10000." using (sa.saleamount <= 10000)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}