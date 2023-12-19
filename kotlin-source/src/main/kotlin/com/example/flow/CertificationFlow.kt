package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.CertificateContract
import com.example.state.CertificateRequestState
import com.example.state.CertificateState
import com.google.common.collect.ImmutableList
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.internal.ResolveTransactionsFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap

object CertificationFlow {

    private val logger = contextLogger()

    @InitiatingFlow
    @StartableByRPC
    class CertificateRequestFlow (val lcNumber: String,
                                  val seller: Party,
                                  val authority: Party,
                                  val commodity: String,
                                  val originFrom : String,
                                  val exportTo: String,
                                  val shippingNo: String ) : FlowLogic<SignedTransaction>()
    {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on Letter of Credit.")
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

            val inputState = CertificateRequestState(lcNumber = this.lcNumber,
                    buyer = this.ourIdentity,
                    seller = this.seller,
                    authority = this.authority,
                    commodity = this.commodity,
                    originFrom = this.originFrom,
                    exportTo = this.exportTo,
                    shippingNo = this.shippingNo,
                    state = "Requested")

            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            progressTracker.currentStep = CertificationFlow.CertificateRequestFlow.Companion.GENERATING_TRANSACTION
            System.out.println("creating transaction for certificate request....  ")


            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(inputState, CertificateContract.CR_CONTRACT_ID )
                    .addCommand(Command(CertificateContract.Commands.Request(), ImmutableList.of(ourIdentity.owningKey)))

            txBuilder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(txBuilder)

            System.out.println("triggering finality flow....  ")
            val requestFinality = subFlow(FinalityFlow(ptx))


           /* System.out.println("building certificate approval transaction........")
            val stateAndRef = ptx.tx.outRefsOfType<CertificateRequestState>().single()
            val approveTransaction = TransactionBuilder(notary)
                    .addInputState(ptx.tx.outRefsOfType<CertificateRequestState>().single())
                    .addOutputState(stateAndRef.state.data.withNewStatus("Approved"), CertificateContract.CR_CONTRACT_ID)
                    .addCommand(Command(CertificateContract.Commands.Accept(), ImmutableList.of(ourIdentity.owningKey, authority.owningKey)))

            approveTransaction.verify(serviceHub)

            System.out.println("signing approval transaction.......")
            val signInitialTransaction = serviceHub.signInitialTransaction(approveTransaction)
            val certFlow = initiateFlow(authority)

            System.out.println("waiting for authority signing....")
            val subFlow = subFlow(CollectSignaturesFlow(signInitialTransaction, ImmutableList.of(certFlow)))

            System.out.println("finalizing approval transaction........")
            val subFlow1 = subFlow(FinalityFlow(subFlow))

*/
            val certFlow = initiateFlow(authority)
            certFlow.send(lcNumber)

            val ptx_1 = certFlow.receive<SignedTransaction>().unwrap{it}
            val stx = serviceHub.addSignature(ptx_1)

            subFlow(ResolveTransactionsFlow(stx, certFlow))

            val finalFlow = subFlow(FinalityFlow(stx))

            val vaultQueryCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
            val queryBy = serviceHub.vaultService.queryBy<CertificateState>(vaultQueryCriteria)

            System.out.println("certificate status....." + queryBy.states.single().state)
            return finalFlow
        }

    }

    @InitiatedBy(CertificationFlow.CertificateRequestFlow::class)
    class CertificateCreation(val initiatorFlow: FlowSession) : FlowLogic<SignedTransaction> ()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            System.out.println("inside creation flow........")

            val vaultQueryCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
            val queryBy = serviceHub.vaultService.queryBy<CertificateRequestState>(vaultQueryCriteria)

            initiatorFlow.receive<String>().unwrap {
                val found = queryBy.states.find { st -> st.state.data.lcNumber.equals(it) }

                require(found != null) { "vault do not have this certificate request" }

                it
            }

            val request = queryBy.states.single() //single() for now , actually we need to find it from vault
            System.out.println("inside creation flow........building transaction")
            val createTransaction = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities[0])
                    .addInputState(request)
                    .addOutputState(request.state.data.withNewStatus("Approved"), CertificateContract.CR_CONTRACT_ID)
                    .addOutputState(CertificateState(
                            request.state.data.lcNumber,
                            request.state.data.buyer,
                            request.state.data.seller,
                            request.state.data.authority,
                            request.state.data.commodity,
                            request.state.data.originFrom,
                            request.state.data.exportTo,
                            request.state.data.shippingNo,
                            "VALID"
                    ), CertificateContract.CR_CONTRACT_ID)
                    .addCommand(Command(CertificateContract.Commands.Create(), ImmutableList.of(initiatorFlow.counterparty.owningKey, ourIdentity.owningKey)))

            System.out.println("inside creation flow........signing transaction")
            val signInitialTransaction = serviceHub.signInitialTransaction(createTransaction)
            initiatorFlow.send(signInitialTransaction)
            System.out.println("inside creation flow........returning signed transaction")
            return waitForLedgerCommit(signInitialTransaction.id)
        }

    }


}