package com.example.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

open class CertificateContract : Contract{
    companion object {
        @JvmStatic
        val CR_CONTRACT_ID = "com.example.contract.CertificateContract"
    }

    override fun verify(tx: LedgerTransaction) {
        System.out.println("inside certificate contract.....")
        val command = tx.commands.requireSingleCommand<CertificateContract.Commands>()
        when (command.value) {
            is CertificateContract.Commands.Request ->
            {}
            is CertificateContract.Commands.Create ->
            {}
            is CertificateContract.Commands.Verify ->
            {}
            is CertificateContract.Commands.Accept ->
            {}
        }
    }

    interface Commands : CommandData {
        class Request: CertificateContract.Commands
        class Create: CertificateContract.Commands
        class Verify: CertificateContract.Commands
        class Accept: CertificateContract.Commands

    }
}