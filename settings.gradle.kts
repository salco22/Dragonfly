pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "Dragonfly"
include(":app")

include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:designsystem")

include(":feature:accounts")
include(":feature:lock")
include(":feature:settings")
