package com.example.server

import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 * Created by maheshgovind on 08/04/18.
 */


fun main(args: Array<String>) {
    com.example.server.RestServer().main(args)
}

private class RestServer {
    companion object {
        val logger: Logger = loggerFor<RestServer>()
    }
    fun main(args: Array<String>) {

    }
}