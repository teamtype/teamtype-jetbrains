# IntelliJ plugin

Make sure you have at least a JDK 21 installed on your machine and that the
`JAVA_HOME` variable points to the JDK's home directory.

## How to run locally (without IDE)

`./gradlew runIde --no-daemon` directly builds & starts a sandboxed IntelliJ
IDEA with the plugin enabled.

## Install into existing IDE

`./gradlew buildPlugin` creates in `build/distributions/` a ZIP archive that
can be installed in IntelliJ's plugin settings with the option “Install Plugin
from Disk”.

## Develop with IntelliJ

Just open the project and use “Run Plugin” in the run drop down.

## Develop with Neovim

- Make sure to enable [`kotlin_language_server` from nvim-lspconfig][nvim-kls].

[nvim-kls]: https://github.com/neovim/nvim-lspconfig/blob/master/doc/configs.md#kotlin_language_server
