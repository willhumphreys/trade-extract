plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "uk.co.threebugs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.cloudera.com/artifactory/ext-release-local/")
    }
    maven {
        name = "RepsyRepository"
        url = uri("https://api.repsy.io/mvn/willhumphreys/default")
        credentials {
            username = System.getenv("REPSY_USERNAME") ?: project.findProperty("repsyUsername") as String? ?: ""
            password = System.getenv("REPSY_PASSWORD") ?: project.findProperty("repsyPassword") as String? ?: ""
        }

    }
}

dependencies {

    // https://mvnrepository.com/artifact/com.hadoop.gplcompression/hadoop-lzo
    implementation("com.hadoop.gplcompression:hadoop-lzo:0.4.20")


    // https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-common
    implementation("org.apache.hadoop:hadoop-common:2.7.3") {
        exclude(group = "commons-beanutils", module = "commons-beanutils")
    }


    implementation("software.amazon.awssdk:s3:2.20.40")
    implementation("commons-cli:commons-cli:1.5.0")

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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "uk.co.threebugs.Runner"
    }
}