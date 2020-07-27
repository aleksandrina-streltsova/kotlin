plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    // Wizard backend is reused in the KMM plugin. Please take a look at https://jetbrains.quip.com/LBjwAw0H3w8H
    // before adding new dependencies on the Kotlin plugin parts.

    implementation(intellijDep()) { includeJars("util") } //needed only for message bundles
    testImplementation(intellijDep()) { includeJars("trove4j") } //needed only for message bundles

    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${property("versions.kotlinx-collections-immutable")}")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()
