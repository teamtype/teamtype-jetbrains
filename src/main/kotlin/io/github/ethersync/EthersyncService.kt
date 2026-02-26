package io.github.ethersync

import kotlinx.coroutines.Job

interface EthersyncService {

   fun start(joinCode: String?): Job

   fun shutdown(): Job

   fun startWithCustomCommandLine(commandLine: String)
}
