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
    watchosArm32 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    watchosArm64 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    watchosX86 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    // Create three targets for watchOS.
    // Create common source sets: anotherWatchosMain and anotherWatchosTest.
    watchos("anotherWatchos") {
        binaries {
            framework {
                baseName = "a"
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
        val watchosArm32Main by getting
        val watchosArm32Test by getting
        val watchosArm64Main by getting
        val watchosArm64Test by getting
        val watchosX86Main by getting
        val watchosX86Test by getting
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
        }
        val watchosMain by creating {
            dependsOn(sharedMain)
            watchosArm32Main.dependsOn(this)
            watchosArm64Main.dependsOn(this)
            watchosX86Main.dependsOn(this)
        }
        val watchosTest by creating {
            dependsOn(sharedTest)
            watchosArm32Test.dependsOn(this)
            watchosArm64Test.dependsOn(this)
            watchosX86Test.dependsOn(this)
        }
    }
}