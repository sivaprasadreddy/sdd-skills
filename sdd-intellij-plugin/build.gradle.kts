plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "dev.sivalabs"
version = "1.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.1.7")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    signPlugin {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        //certificateChainFile = file("certs/chain.crt")
        //privateKeyFile = file("certs/private.pem")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}