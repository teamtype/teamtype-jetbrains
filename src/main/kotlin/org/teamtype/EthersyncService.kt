package org.teamtype

import kotlinx.coroutines.Job

interface EthersyncService {

   fun start(joinCode: String?): Job

   fun shutdown(): Job

   fun startWithCustomCommandLine(commandLine: String)
}
