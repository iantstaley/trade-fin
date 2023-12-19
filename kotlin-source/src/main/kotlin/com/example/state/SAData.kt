package com.example.state

import javax.persistence.Column

/**
 * Created by mkshibu on 28/03/18.
 */
/*
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
 */
data class SAData(
        val id: String,
        val state : String,
        val seller:String,
        val buyer :String,
        val amount:Int,
        val itemofsale:String,
        val sellercountry:String,
        val buyercountry:String,
        val agreementDate:String,
        val quantityofsale:String,
        val notarisedby:String,
        val countryofnotary:String

)