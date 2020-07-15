plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
kotlin {
    iosArm64 {
        binaries {
            framework {
                baseName = "ios"
            }
        }
    }
    iosX64 {
        binaries {
            framework {
                baseName = "ios"
            }
        }
    }
    // Create three targets for watchOS.
    // Create common source sets: watchosMain and watchosTest.
    watchos {
        binaries {
            framework {
                baseName = "watchos"
            }
        }
    }
    // Create two targets for tvOS.
    // Create common source sets: tvosMain and tvosTest.
    tvos {
        binaries {
            framework {
                baseName = "tvos"
            }
        }
    }
    // Create two targets for iOS.
    // Create common source sets: anotherIosMain and anotherIosTest.
    ios("anotherIos") {
        binaries {
            framework {
                baseName = "anotherIos"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val iosArm64Main by getting
        val iosArm64Test by getting
        val iosX64Main by getting
        val iosX64Test by getting
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
        }
        val iosMain by creating {
            dependsOn(sharedMain)
            iosArm64Main.dependsOn(this)
            iosX64Main.dependsOn(this)
        }
        val iosTest by creating {
            dependsOn(sharedTest)
            iosArm64Test.dependsOn(this)
            iosX64Test.dependsOn(this)
        }
    }
}