package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CertificationSchema

object CertificationSchmeaV1 : MappedSchema(
        schemaFamily = CertificationSchema.javaClass,
        version = 1,
        mappedTypes = listOf(CertificationSchmeaV1.PersistentRequest::class.java, CertificationSchmeaV1.PersistentCerts::class.java)) {

    @Entity
    @Table(name = "certreq_states")
    class PersistentRequest (
            @Column(name = "lcnumber")
            val lcNumber: String,
            @Column(name = "buyer")
            val buyer: String,
            @Column(name = "seller")
            val seller: String,
            @Column(name = "certificate_authority")
            val authority: String,
            @Column(name = "commodity_name")
            val commodity: String,
            @Column(name = "export_from")
            val originFrom : String,
            @Column(name = "export_to")
            val exportTo: String,
            @Column(name = "shipping_billing_no")
            val shippingNo: String,
            @Column(name = "request_state")
            val state: String,
            @Column(name = "linear_id")
            val linearId: UUID
    ) : PersistentState() {
        constructor(): this("",  "", "", "", "","","", "", "", UUID.randomUUID())
    }

    @Entity
    @Table(name = "cert_states")
    class PersistentCerts (
            @Column(name = "lcnumber")
            val lcNumber: String,
            @Column(name = "buyer")
            val buyer: String,
            @Column(name = "seller")
            val seller: String,
            @Column(name = "certificate_authority")
            val authority: String,
            @Column(name = "commodity_name")
            val commodity: String,
            @Column(name = "export_from")
            val originFrom : String,
            @Column(name = "export_to")
            val exportTo: String,
            @Column(name = "shipping_billing_no")
            val shippingNo: String,
            @Column(name = "request_state")
            val state: String,
            @Column(name = "linear_id")
            val linearId: UUID
    ) : PersistentState() {
        constructor(): this("",  "", "", "", "","","", "", "", UUID.randomUUID())
    }
}