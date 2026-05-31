pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ABS Client App"
include(
    ":app",
    ":core:model",
    ":core:preferences",
    ":core:database",
    ":core:network",
    ":core:player",
    ":domain",
    ":data",
    ":feature:login:api",
    ":feature:login:impl",
    ":feature:library:api",
    ":feature:library:impl",
    ":feature:player:api",
    ":feature:player:impl",
    ":feature:home:api",
    ":feature:home:impl",
    ":feature:androidauto",
    ":feature:miniplayer"
)
