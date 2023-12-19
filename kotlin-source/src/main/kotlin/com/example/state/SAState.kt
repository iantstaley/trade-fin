package com.example.state

import com.example.schema.SASchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording SA(Sale Agreement) agreements between two parties. - Seller and Buyer
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the SA(Sale Agreement).
 * @param Buyer  the party initiating the SA (sale agreement)
 * @param Seller the party receiving and approving the SA
 */
/*

Sale Agreement
{
"SaleAgreementNo":"SA1234",
"Buyer":"FutureGroup",
"Seller":"ADL",
"BuyerCountry" : "India",
"SellerCountry" : "USA",
"CountryOfNotary" : "Germany",
"NotarisedBy" : "ABC GmBH",
"ItemOfSale" : "Hazel Nut",
"QuantityOfSale" : "500 MT",
"SaleAmount" : 5000,
"AgreementDate" : "01/01/2018"
"State": "Triggered",

}
 */
data class SAState(val saleamount: Int,
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
                   override val linearId: UniqueIdentifier = UniqueIdentifier(externalId = saleagreementno)):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(seller, buyer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SASchemaV1 -> SASchemaV1.PersistentSA(
                    this.seller.name.toString(),
                    this.buyer.name.toString(),
                    this.saleamount,
                    this.state,
                    this.saleagreementno,
                    this.itemofsale,
                    this.sellercountry,
                    this.buyercountry,
                    this.agreementDate,
                    this.quantityofsale,
                    this.notarisedby,
                    this.countryofnotary,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SASchemaV1)
    fun withNewStatus(newStatus: String) = copy(state = newStatus)
}
