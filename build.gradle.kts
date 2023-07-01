import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

group = "net.dertod2"
version = "1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.mysql:mysql-connector-j:8.0.33")
    implementation("com.google.guava:guava:32.1.1-jre")
}

val fatJar = task("fatJar", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Implementation-Title"] = "DatabaseHandler Professional"
        attributes["Implementation-Version"] = version
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {

    "build" {
        dependsOn(fatJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "DatabaseHandlerPro"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("My Library")
                description.set("A concise description of my library")
                url.set("http://www.example.com/library")
                developers {
                    developer {
                        id.set("DerTod2")
                        name.set("Nico-Steven Schopnie")
                        email.set("dertodzwei@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/DerTod2/DatabaseHandlerPro.git")
                    developerConnection.set("scm:git:ssh://github.com/DerTod2/DatabaseHandlerPro.git")
                    url.set("https://github.com/DerTod2/DatabaseHandlerPro")
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri("https://exusware.net/repository/releases/")
            val snapshotsRepoUrl = uri("https://exusware.net/repository/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}