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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // THÊM DÒNG NÀY ĐỂ TẢI JMOBI VÀ CÁC THƯ VIỆN ĐỌC FILE
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases") }
    }
}

rootProject.name = "TextStoryReader"
include(":app")