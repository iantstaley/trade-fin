package com.example.contract

import com.example.contract.IRContract.Companion.IR_CONTRACT_ID
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IRContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun `transaction must include Create command`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IRContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                input(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IRContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an ir.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IRContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                command(miniCorp.publicKey, IRContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"

        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                command(megaCorp.publicKey, IRContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        val ir = 1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, megaCorp.party, megaCorp.party,irno,irstat))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IRContract.Commands.Create())
                `fails with`("The bank and the exchange house cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IRs`() {
        val ir = -1
        val irno ="123"
        val irstat ="triggered"
        ledgerServices.ledger {
            transaction {
                output(IR_CONTRACT_ID, IRState(ir, miniCorp.party, megaCorp.party,irno,irstat))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IRContract.Commands.Create())
                `fails with`("The ir's value must be non-negative.")
            }
        }
    }
}