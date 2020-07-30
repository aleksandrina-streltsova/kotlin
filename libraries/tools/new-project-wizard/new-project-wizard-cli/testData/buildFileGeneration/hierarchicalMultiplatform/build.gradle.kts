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
    js {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    macosX64()
    iosX64 {
        binaries {
            framework {
                baseName = "a"
            }
        }
    }
    linuxX64()
    mingwX64()
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
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val macosX64Main by getting
        val macosX64Test by getting
        val iosX64Main by getting
        val iosX64Test by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        val mingwX64Main by getting
        val mingwX64Test by getting
        val sharedMain by creating {
            dependsOn(commonMain)
            jvmMain.dependsOn(this)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
            jvmTest.dependsOn(this)
        }
        val nativeMain by creating {
            dependsOn(sharedMain)
        }
        val nativeTest by creating {
            dependsOn(sharedTest)
        }
        val nativeDarwinMain by creating {
            dependsOn(nativeMain)
            macosX64Main.dependsOn(this)
            iosX64Main.dependsOn(this)
        }
        val nativeDarwinTest by creating {
            dependsOn(nativeTest)
            macosX64Test.dependsOn(this)
            iosX64Test.dependsOn(this)
        }
        val nativeOtherMain by creating {
            dependsOn(nativeMain)
            linuxX64Main.dependsOn(this)
            mingwX64Main.dependsOn(this)
        }
        val nativeOtherTest by creating {
            dependsOn(nativeTest)
            linuxX64Test.dependsOn(this)
            mingwX64Test.dependsOn(this)
        }
    }
}