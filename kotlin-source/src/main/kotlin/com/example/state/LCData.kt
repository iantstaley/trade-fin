

package com.example.state

import javax.persistence.Column


/*data class LCState(val lcmount: Int,
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
        */

data class  LCData(
        val lcamt:Int,
    val seller:String,
    val buyer:String,
    val issuebank:String,
    val advbank:String,
    val lctype:String,
    val lcdate:String,
    val lcnum:String,
    val lcexpiry:String,
    val lcdescrp:String,
    val state:String,
    val sano:String


)