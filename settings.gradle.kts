pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tessera"

// Core library
include(":tessera")
include(":tessera-hilt")

// Sample app - classic single-NavHost setup
include(":sample:app")

// Sample app - multi-NavHost setup demonstrating MultitabState
include(":sample:app-multitab")

include(":sample:feature-home")
include(":sample:feature-detail")
include(":sample:feature-settings")
include(":sample:feature-checkout")
