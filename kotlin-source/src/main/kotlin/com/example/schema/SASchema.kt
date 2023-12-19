package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for SAState.
 */
object SASchema

/**
 * An SAState schema.
val saleamount: Int,
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
 */
object SASchemaV1 : MappedSchema(
        schemaFamily = SASchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSA::class.java)) {
    @Entity
    @Table(name = "sa_states")
    class PersistentSA(
            @Column(name = "seller")
            var Seller: String,

            @Column(name = "buyer")
            var Buyer: String,

            @Column(name = "saleamount")
            var Saleamount: Int,

            @Column(name="saleagreementno")
            var Saleagreementno:String,

            @Column(name="state")
            var State:String,

            @Column(name="itemofsale")
            var Itemofsale:String,

            @Column(name="sellercountry")
            var Sellercountry:String,

            @Column(name="buyercountry")
            var Buyercountry:String,

            @Column(name="agreementDate")
            var AgreementDate:String,

            @Column(name="quantityofsale")
            var Quantityofsale:String,

            @Column(name="notarisedby")
            var Notarisedby:String,

            @Column(name="countryofnotary")
            var Countryofnotary:String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "",0,  "",  "", "",

                "", "", "","","",
                "",  UUID.randomUUID())
    }
}