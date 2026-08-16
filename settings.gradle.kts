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
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.FaceOnLive")
                includeGroup("com.github.philburk")
                includeGroup("com.github.racra")
                includeGroup("com.github.tdlibx")
                includeGroup("com.github.TeamNewPipe")
            }
        }
    }
}

rootProject.name = "PixelPlay"
include(":app")
include(":shared")
include(":wear")
include(":baselineprofile")

val archiveTuneCore = file("backend-refences/ArchiveTune/core")
val archiveTuneDeobfuscator = file("backend-refences/ArchiveTune/morideobfuscator")
val archiveTuneExtractor = file("backend-refences/ArchiveTune/moriextractor")

if (!archiveTuneCore.exists() || !archiveTuneDeobfuscator.exists() || !archiveTuneExtractor.exists()) {
    try {
        val process = ProcessBuilder("git", "submodule", "update", "--init", "--recursive")
            .directory(rootDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
    } catch (_: Throwable) {
        // Fall through to directory check
    }
}

if (!archiveTuneCore.exists() || !archiveTuneDeobfuscator.exists() || !archiveTuneExtractor.exists()) {
    throw GradleException(
        """
        ====================================================================================
        [PixelPlayer Build Error] Missing Git Submodule: backend-refences/ArchiveTune
        
        The subprojects ':core', ':morideobfuscator', and ':moriextractor' were not found in:
          ${archiveTuneCore.parentFile?.absolutePath}
        
        To fix this issue:
        1. LOCAL MACHINE / ANOTHER DEVICE:
           Run in your terminal:
             git submodule update --init --recursive
           (or clone using: git clone --recurse-submodules <repo_url>)
           
        2. GITLAB CI (.gitlab-ci.yml):
           Add this variable to your .gitlab-ci.yml:
             variables:
               GIT_SUBMODULE_STRATEGY: recursive
             
        3. GITHUB ACTIONS (.github/workflows/*.yml):
           Ensure your checkout step has 'submodules: recursive':
             - uses: actions/checkout@v4
               with:
                 submodules: recursive
                 
        4. BITBUCKET PIPELINES (bitbucket-pipelines.yml):
           Enable submodules under clone:
             clone:
               submodules: true
               
        5. DOCKER / CUSTOM CLOUD CI:
           Run 'git submodule update --init --recursive' before running './gradlew'
        ====================================================================================
        """.trimIndent()
    )
}

include(":core")
project(":core").projectDir = archiveTuneCore

include(":morideobfuscator")
project(":morideobfuscator").projectDir = archiveTuneDeobfuscator

include(":moriextractor")
project(":moriextractor").projectDir = archiveTuneExtractor
