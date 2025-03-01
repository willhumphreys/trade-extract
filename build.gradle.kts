plugins {
    java
}

group = "uk.co.threebugs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.cloudera.com/artifactory/ext-release-local/")
    }
    maven {
        url = uri("https://maven.twttr.com")
    }
}

dependencies {
    // Hadoop dependencies from pom.xml
    implementation("com.hadoop.gplcompression:hadoop-lzo:0.4.20")
    implementation("org.apache.hadoop:hadoop-common:3.3.0")

    // Lombok (updated to version 1.18.34 to match pom.xml)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    // JUnit Jupiter (using BOM for version alignment; update version to 5.10.3 if needed)
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // AssertJ for fluent assertions (updated to 3.26.3 as in pom.xml)
    testImplementation("org.assertj:assertj-core:3.26.3")

    // Mockito dependencies from pom.xml
    testImplementation("org.mockito:mockito-core:5.14.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// Optionally configure the JAR task with a Main-Class entry
tasks.jar {
    manifest {
        attributes["Main-Class"] = "uk.co.threebugs.Main" // Replace with your actual main class
    }
}
