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
include(":core:testing")
include(":core:ui")
include(":core:work")

include(":feature:accounts")
include(":feature:bills")
include(":feature:budgets")
include(":feature:classification")
include(":feature:lock")
include(":feature:settings")
include(":feature:transactions")
