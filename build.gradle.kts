import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "2.0.21"
    id("maven-publish")
}

group = "be.nijverheidskaai"
version = "0.1.0"

kotlin {
    jvmToolchain(17)

    jvm()

    val xcf = XCFramework("Routeberekening")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "Routeberekening"
            isStatic = true
            xcf.add(this)
        }
    }
}
