pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ToolBoxPro"
include(":app")
include(":admin")
