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
    tvosArm64 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    tvosX64 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    // Create two targets for tvOS.
    // Create common source sets: anotherTvosMain and anotherTvosTest.
    tvos("anotherTvos") {
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
        val tvosArm64Main by getting
        val tvosArm64Test by getting
        val tvosX64Main by getting
        val tvosX64Test by getting
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
        }
        val tvosMain by creating {
            dependsOn(sharedMain)
            tvosArm64Main.dependsOn(this)
            tvosX64Main.dependsOn(this)
        }
        val tvosTest by creating {
            dependsOn(sharedTest)
            tvosArm64Test.dependsOn(this)
            tvosX64Test.dependsOn(this)
        }
    }
}