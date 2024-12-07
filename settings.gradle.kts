pluginManagement {
    repositories {
        maven("https://maven.wagyourtail.xyz/snapshots")
        gradlePluginPortal {
            content {
                excludeGroup("org.apache.logging.log4j")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}

rootProject.name = "zson"

