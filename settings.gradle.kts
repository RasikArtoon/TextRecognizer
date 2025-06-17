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
       maven { url = uri("https://jitpack.io") }
        maven { url =uri("https://www.leadtools.com/rd/v230/android-repo") }
    }

}

rootProject.name = "Card Recogization"
include(":app")
include(":sdk")
