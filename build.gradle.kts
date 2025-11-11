import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
   id("java")
   // Do not upgrade until following has been fixed:
   // https://github.com/kotlin-community-tools/kotlin-language-server/pull/17
   id("org.jetbrains.kotlin.jvm") version "2.0.21"
   id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "io.github.ethersync"
version = "0.7.0-SNAPSHOT"

repositories {
   mavenCentral()

   intellijPlatform {
      defaultRepositories()
   }
}

dependencies {
   intellijPlatform {
      intellijIdeaCommunity("2024.3.1.1")
      bundledPlugin("org.jetbrains.plugins.terminal")

      testFramework(TestFrameworkType.Platform)
   }

   implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
   implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.23.1")

   testImplementation("junit:junit:4.13.2")
   testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.1")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.1")
   testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

kotlin {
   compilerOptions {
      jvmToolchain(21)
   }
}

tasks {
   patchPluginXml {
      sinceBuild.set("243")
   }

   test {
      useJUnitPlatform {
         includeEngines("junit-vintage")
      }
   }
}
