pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "Trigger"

include(":app")
include(":core:common")
include(":core:strings")
include(":core:ui")
include(":domain")
include(":data")
include(":feature:auth")
include(":feature:home")
include(":feature:chat")
include(":feature:profile")
