plugins {
    kotlin("multiplatform")
    id("jps-compatible")
}

/*dependencies {
    // Wizard backend is reused in the KMM plugin. Please take a look at https://jetbrains.quip.com/LBjwAw0H3w8H
    // before adding new dependencies on the Kotlin plugin parts.
    api("org.apache.velocity:velocity:1.7") // we have to use the old version as it is the same as bundled into IntelliJ

    //needed only for message bundles
    implementation(intellijDep()) { includeJars("util") }

    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${property("versions.kotlinx-collections-immutable")}")
}*/

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src")
            resources.srcDir("resources")
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:${property("versions.kotlinx-collections-immutable")}")
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("jvm/src")
            dependencies {
                api("org.apache.velocity:velocity:1.7") // we have to use the old version as it is the same as bundled into IntelliJ

                //needed only for message bundles
                implementation(intellijDep()) { includeJars("util") }
            }
        }
        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
    }
}

/*sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()*/
