pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "isoluminol"

include("isoluminol-api")
include("isoluminol-server")
include("isoluminol-checkstyle")

gradle.lifecycle.beforeProject {
    val mcVersion = providers.gradleProperty("mcVersion").get().trim()
    val isoluminolVersionChannel = providers.gradleProperty("channel").get().trim()
    val isoluminolBuildNumber = providers.environmentVariable("BUILD_NUMBER").orNull?.trim()?.toInt()
    val versionString = if (isoluminolBuildNumber == null) {
        "$mcVersion.local-SNAPSHOT"
    } else {
        "$mcVersion.build.$isoluminolBuildNumber-${isoluminolVersionChannel.lowercase()}"
    }
    version = versionString
}
