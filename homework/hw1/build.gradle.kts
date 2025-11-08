plugins {
    kotlin("jvm") version "2.2.20"
    application
}

group = "ru.msk.xls"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:3.5.6")

    implementation("io.insert-koin:koin-core:3.5.6")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}

application {
    mainClass.set("ru.msk.xls.MainKt")
}

tasks.named("run", JavaExec::class) {
    standardInput = System.`in`
}
