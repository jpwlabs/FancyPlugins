plugins {
    id("com.gradleup.shadow") version "9.3.1" apply false
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id("de.eldoria.plugin-yml.paper") version "0.8.0" apply false
}

allprojects {
    group = "de.oliver"
    description = "Minecraft plugins of FancyInnovations"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.fancyinnovations.com/releases")
        maven(url = "https://maven.fancyspaces.net/fancyinnovations/releases")
        maven(url = "https://jitpack.io")
    }

    configurations.configureEach {
        // The pinned Paper 1.21.8 dev-bundle snapshot omits this module's
        // version in its Gradle metadata. Resolve the exact runtime version.
        resolutionStrategy.force("net.kyori:adventure-text-serializer-ansi:4.24.0")
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
