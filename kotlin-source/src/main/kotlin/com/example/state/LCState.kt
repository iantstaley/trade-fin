package com.example.state

import com.example.schema.LCSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording LC(Letter of Cerdit) agreements between four parties. - Buyer, IssuingBank, AdvisingBank and Seller
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the LC(Letter of Cerdit).
 * @param Buyer the party initiating  the LC request
 * @param IssuingBank  the party initiating the  LC(Letter of Cerdit)
 * @param AdvisingBank the party receiving and approving the LC
 * @param Seller the party accepting the LC
 *
 */
/*

Letter of Credit
{
"LCNumber" : LC1234,
"LCType" : "Revokable",
"LCDate" : "01/01/2018",
"LCApplicant" : "FutureGroup",
"LCBeneficiary" : "ADL Corp",
"IssuingBank" : "SBI",
"AdvisingBank" : "Bank of America",
"DescritionOfGoods" : " 500 MT First grade Hazel Nut",
"LCAmount" : "50000 USD",
"LCExpiry" : "01/01/2019",
"State": "Requested"

}


 */
data class LCState(val lcmount: Int,
                   val seller: Party,
                   val buyer: Party,
                   val issuingbank: Party,
                   val advisingbank: Party,
                   val lctype:String,
                   val lcdate:String,
                   val lcnumber:String,
                   val lcexpiry:String,
                   val description:String,
                   val state:String,
                   val sano:String,
                   override val linearId: UniqueIdentifier = UniqueIdentifier(externalId = lcnumber)):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(seller, buyer,issuingbank,advisingbank)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LCSchemaV1 -> LCSchemaV1.PersistentLC(
                    this.seller.name.toString(),
                    this.buyer.name.toString(),
                    this.issuingbank.name.toString(),
                    this.advisingbank.name.toString(),
                    this.lcmount,
                    this.lctype,
                    this.state,
                    this.description,
                    this.lcdate,
                    this.lcexpiry,
                    this.sano,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LCSchemaV1)
    fun withNewStatus(newStatus: String) = copy(state = newStatus)
}
