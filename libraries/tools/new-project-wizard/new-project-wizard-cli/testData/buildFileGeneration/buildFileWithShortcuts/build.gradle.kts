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
    iosArm64("iosArm64_1") {
        binaries {
            framework {
                baseName = "ios_1"
            }
        }
    }
    iosX64("iosX64_1") {
        binaries {
            framework {
                baseName = "ios_1"
            }
        }
    }
    watchos {
        binaries {
            framework {
                baseName = "watchos"
            }
        }
    }
    tvos {
        binaries {
            framework {
                baseName = "tvos"
            }
        }
    }
    ios {
        binaries {
            framework {
                baseName = "ios"
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
        val iosArm64_1Main by getting
        val iosArm64_1Test by getting
        val iosX64_1Main by getting
        val iosX64_1Test by getting
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
        }
        val ios_1Main by creating {
            dependsOn(sharedMain)
            iosArm64_1Main.dependsOn(this)
            iosX64_1Main.dependsOn(this)
        }
        val ios_1Test by creating {
            dependsOn(sharedTest)
            iosArm64_1Test.dependsOn(this)
            iosX64_1Test.dependsOn(this)
        }
    }
}