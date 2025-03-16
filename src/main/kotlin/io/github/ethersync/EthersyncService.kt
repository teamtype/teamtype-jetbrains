package io.github.ethersync

interface EthersyncService {

   fun start(peer: String?)

   fun shutdown()

   fun startWithCustomCommandLine(commandLine: String)
}
