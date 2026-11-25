pluginManagement {
    includeBuild("build-logic")
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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "android-syndicate"

include(":app")

include(":feature:feeds")
include(":feature:articles")
include(":feature:reader")
include(":feature:settings")

include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:common")
include(":core:testing")
