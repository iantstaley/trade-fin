package com.example.flow

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import net.corda.core.utilities.contextLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IRFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    val irinst = IRState(1,a.info.singleIdentity(),b.info.singleIdentity(),"triggered","123")
    private val logger = contextLogger()

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract"))
        a = network.createPartyNode()
        b = network.createPartyNode()


        //irinst.value=1
        //irinst.bank = a!!
        //irinst.exchangehouse = b!!
        //irinst.IRStatus = "triggered"
        //irinst.IRNo ="123"
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(IRRequestFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid IRs`() {
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()
        logger.info("flow rejects invalid IRs fun")
        // The IOUContract specifies that IRs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(a.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storages`() {
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input IR`() {
        val irValue = 1
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as IRState
            assertEquals(recordedState.value, irValue)
            assertEquals(recordedState.bank, a.info.singleIdentity())
            assertEquals(recordedState.exchangehouse, b.info.singleIdentity())
        }
    }

    @Test
    fun `flow records the correct IR in both parties' vaults`() {
        val irValue = 1
        val flow = IRRequestFlow.Initiator(irinst)
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded IR in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val IRs = node.services.vaultService.queryBy<IRState>().states
                assertEquals(1, IRs.size)
                val recordedState = IRs.single().state.data
                assertEquals(recordedState.value, irValue)
                assertEquals(recordedState.bank, a.info.singleIdentity())
                assertEquals(recordedState.exchangehouse, b.info.singleIdentity())
            }
        }
    }
}