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
    implementation("org.http4k:http4k-template-handlebars:5.35.0.0")
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.55.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation(kotlin("test"))
    testImplementation("org.http4k:http4k-testing-hamkrest:5.35.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
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
