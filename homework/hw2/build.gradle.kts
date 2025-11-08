plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.google.devtools.ksp") version "2.3.2"
    application
}

group = "ru.msk.xls"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.sqlite.jdbc)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.csv)

    implementation(libs.koin.core)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.clikt)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

application {
    mainClass = "ru.msk.xls.kpo.bank.MainKt"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named("run", JavaExec::class) {
    standardInput = System.`in`
}

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles a runnable fat JAR with all dependencies."

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}
