package io.github.ethersync

interface EthersyncService {

   fun start(joinCode: String?)

   fun shutdown()

   fun startWithCustomCommandLine(commandLine: String)
}
