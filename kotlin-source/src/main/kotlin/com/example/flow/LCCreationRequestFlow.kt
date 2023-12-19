package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.LCContract
import com.example.state.SAState
import com.example.state.LCState
import net.corda.core.contracts.Command
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
object LCCreationRequestFlow {
    private val logger = contextLogger()
    @InitiatingFlow
    @StartableByRPC
    class Initiator( val lcState : LCState
             /*    val lcmount: Int,
                   val seller: Party,
                   val buyer: Party,
                   val issuingbank: Party,
                   val advisingbank: Party,
                   val lctype:String,
                   val lcdate:String,
                   val lcnumber:String,
                   val lcexpiry:String,
                   val description:String,
                   val state:String
                    */



    ) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for a new Letter of Credit.")
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
        override fun call(): SignedTransaction  {


            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            progressTracker.currentStep = LCCreationRequestFlow.Initiator.Companion.GENERATING_TRANSACTION
            logger.info("Verifying the state of SA input for LC create transaction  ")

            //Key step before LC creation. before creating the LC we need to verify that passed sale agreement
            // is notarised. Note that we are not passing the SAstate as an inputstate for LC creation.

            //query saleAgreement by external saleagreementno
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(externalId = listOf(lcState.sano))
            val saState = serviceHub.vaultService.queryBy(SAState::class.java, queryCriteria).states.single()
            val state = saState.state.data
            if(state.state != "Notarised") {
                logger.info("Sale Agreement state is ",saState.state)
                throw IllegalArgumentException("Sale Agreement state is " + saState.state)
            }
            //logger.info("The result {}", saState)



           // val txCommand = Command(LCContract.Commands.Request(), lcState.participants.map { it.owningKey })
            val txCommand = Command(LCContract.Commands.Request(), lcState.issuingbank.owningKey)
            logger.info("Transaction builder   ")
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(lcState, LCContract.LC_CONTRACT_ID )
                    .addCommand(txCommand)

            txBuilder.verify(serviceHub)
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
            val otherPartyFlow = initiateFlow(lcState.issuingbank!!)
            logger.info("initiating flow   ")
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(otherPartyFlow)))
            return subFlow(FinalityFlow(fullySignedTx ))

        }
    }

    @InitiatedBy(LCCreationRequestFlow.Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    val output = stx.tx.outputs.single().data
                    "This must be an LC transaction." using (output is LCState)
                    val lc = output as LCState
                    "I won't accept LC with a LCValue over 10000." using (lc.lcmount <= 10000)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}