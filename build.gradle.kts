plugins {
    kotlin("jvm") version "1.7.10"
    application // Added application plugin
    distribution // Added distribution plugin
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.http4k:http4k-core:4.20.0.0")
    implementation("org.http4k:http4k-server-jetty:4.20.0.0")
    implementation("org.http4k:http4k-template-handlebars:4.20.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

application {
    mainClass.set("AppKt") // Replace with the fully qualified name of your main class
}

distributions {
    main {
        contents {
            from("src/main/resources") // Include resources in the distribution
            from(tasks.getByName("jar")) // Include the JAR file
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
