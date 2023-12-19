package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * The family of schemas for IRState.
 */
object LCSchema
/*
val lcmount: Int,
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
val scno:String,
override val linearId: UniqueIdentifier = UniqueIdentifier(externalId = lcnumber)):
 */
object LCSchemaV1 : MappedSchema(
        schemaFamily = LCSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentLC::class.java)) {
    @Entity
    @Table(name = "lc_states")
    class PersistentLC(
            @Column(name = "seller")
            var Seller: String,

            @Column(name = "buyer")
            var Buyer: String,

            @Column(name = "issuingbank")
            var Issuingbank: String,

            @Column(name="advisingbank")
            var Advisingbank:String,

            @Column(name="lcmount")
            var Lcmount:Int,

            @Column(name="lctype")
            var Lctype:String,

            @Column(name="lcnumber")
            var Lcnumber:String,

            @Column(name="description")
            var Description:String,

            @Column(name="state")
            var State:String,

            @Column(name="lcdate")
            var Lcdate:String,

            @Column(name="scno")
            var Scno:String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "","",  "",  0, "",

                "", "", "","","", UUID.randomUUID())
    }
}