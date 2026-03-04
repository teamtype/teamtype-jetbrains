package org.teamtype

import kotlinx.coroutines.Job

interface TeamtypeService {

   fun start(joinCode: String?): Job

   fun shutdown(): Job

   fun startWithCustomCommandLine(commandLine: String)
}
