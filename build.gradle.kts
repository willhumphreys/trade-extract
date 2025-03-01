plugins {
    java
}

group = "uk.co.threebugs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JUnit Jupiter (using a BOM for version alignment)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // AssertJ for fluent assertions in tests
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Lombok dependencies
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testCompileOnly("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}

// Optionally, configure the JAR task to include the Main-Class attribute in the manifest.
tasks.jar {
    manifest {
        attributes["Main-Class"] = "uk.co.threebugs.Main" // Replace with your actual main class
    }
}
