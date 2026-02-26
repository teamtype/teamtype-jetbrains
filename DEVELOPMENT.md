# IntelliJ plugin

Make sure you have at least a JDK 21 installed on your machine and that the
`JAVA_HOME` variable points to the JDK's home directory.

## How to run locally (without IDE)

`./gradlew runIde --no-daemon` directly builds & starts a sandboxed IntelliJ
IDEA with the plugin enabled.

Testing is built on top of Jetbrains SDK, such as `HeavyPlatfromTestCase`, so
one can test the code base via:

```bash
./gradlew test --rerun
```

## Install into existing IDE

`./gradlew buildPlugin` creates in `build/distributions/` a ZIP archive that
can be installed in IntelliJ's plugin settings with the option “Install Plugin
from Disk”.

## Develop with IntelliJ

Just open the project and use “Run Plugin” in the run drop down.

## Develop with Neovim

Make sure to enable [`kotlin_lsp` from nvim-lspconfig][nvim-kls] to use the
official Kotlin language server.

> [!WARNING] Support for IntelliJ IDEA plugin development is not fully
> implemented by the official Kotlin LSP yet, see
> [#112](https://github.com/Kotlin/kotlin-lsp/issues/112), limiting the
> completion capabilities of the LSP. For example, the Jetbrains imports cannot
> be resolved.

[nvim-kls]: https://github.com/neovim/nvim-lspconfig/blob/master/doc/configs.md#kotlin_lsp
