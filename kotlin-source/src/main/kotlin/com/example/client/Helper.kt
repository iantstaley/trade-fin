package com.example.client

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.FileReader

fun ConfigReader() : String{


    val parser = JSONParser()
    val corebank = parser
            .parse(FileReader("bank.json")) as JSONObject
    val CBurl = corebank.get("corebankurl").toString()


    return CBurl


}



