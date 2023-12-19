package com.example.state

import com.example.schema.CertificationSchmeaV1
import com.google.common.collect.ImmutableList
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class CertificateState (val lcNumber: String,
                             val buyer: Party,
                             val seller: Party,
                             val authority: Party,
                             val commodity: String,
                             val originFrom : String,
                             val exportTo: String,
                             val shippingNo: String,
                             val state: String,
                             override val linearId: UniqueIdentifier =  UniqueIdentifier(externalId = lcNumber)):
LinearState, QueryableState {
    override val participants: List<AbstractParty>
        get() = ImmutableList.of(buyer, seller, authority)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema) {
            is CertificationSchmeaV1 -> CertificationSchmeaV1.PersistentCerts (
                    this.lcNumber,
                    this.buyer.name.toString(),
                    this.seller.name.toString(),
                    this.authority.name.toString(),
                    this.commodity,
                    this.originFrom,
                    this.exportTo,
                    this.shippingNo,
                    this.state,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema>  = listOf(CertificationSchmeaV1)

    fun withNewStatus(newStatus: String) = copy(state = newStatus)

}