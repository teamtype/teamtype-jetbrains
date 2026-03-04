import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
   id("java")
   alias(libs.plugins.kotlin) // Kotlin support
   alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
   alias(libs.plugins.testLogger)
}

group = "org.teamtype"

version = "0.9.0-SNAPSHOT"

repositories {
   mavenCentral()

   intellijPlatform { defaultRepositories() }
}

dependencies {
   intellijPlatform {
      intellijIdea("2025.3.1.1")
      bundledPlugin("org.jetbrains.plugins.terminal")

      testFramework(TestFrameworkType.Platform)
   }

   implementation(libs.lsp4j)
   implementation(libs.lsp4jJsonrpc)

   testImplementation(libs.junit)
   testImplementation(libs.opentest4j)
}

kotlin { compilerOptions { jvmToolchain(21) } }

intellijPlatform {
   pluginConfiguration {
      ideaVersion {
         sinceBuild = "234"
      }
   }
}
