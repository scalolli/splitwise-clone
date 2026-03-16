plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.http4k:http4k-core:5.35.0.0")
    implementation("org.http4k:http4k-format-jackson:5.35.0.0")
    implementation("org.http4k:http4k-server-jetty:5.35.0.0")

    testImplementation(kotlin("test"))
    testImplementation("org.http4k:http4k-testing-hamkrest:5.35.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "com.splitwise.AppKt"
}

tasks.test {
    useJUnitPlatform()
}
