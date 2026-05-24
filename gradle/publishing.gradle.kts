// Shared maven-publish configuration for library modules.
//
// Applied from a module's build.gradle.kts via:
//     apply(from = rootProject.file("gradle/publishing.gradle.kts"))
//
// Requires the `maven-publish` plugin to be applied by the consumer module
// and the Android library `publishing { singleVariant("release") { withSourcesJar() } }`
// block to be configured.
//
// Reads POM metadata from gradle.properties:
//   GROUP, VERSION_NAME, POM_NAME, POM_DESCRIPTION, POM_URL,
//   POM_LICENSE_NAME, POM_LICENSE_URL, POM_LICENSE_DIST,
//   POM_SCM_URL, POM_SCM_CONNECTION, POM_SCM_DEV_CONNECTION,
//   POM_DEVELOPER_ID, POM_DEVELOPER_NAME, POM_DEVELOPER_URL
//
// The module's artifactId is its Gradle name (e.g. "tessera", "tessera-hilt").

extensions.configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            groupId = project.findProperty("GROUP") as String
            artifactId = project.name
            version = project.findProperty("VERSION_NAME") as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set(project.findProperty("POM_NAME") as String? ?: project.name)
                description.set(project.findProperty("POM_DESCRIPTION") as String? ?: "")
                url.set(project.findProperty("POM_URL") as String? ?: "")

                licenses {
                    license {
                        name.set(project.findProperty("POM_LICENSE_NAME") as String? ?: "")
                        url.set(project.findProperty("POM_LICENSE_URL") as String? ?: "")
                        distribution.set(project.findProperty("POM_LICENSE_DIST") as String? ?: "repo")
                    }
                }

                developers {
                    developer {
                        id.set(project.findProperty("POM_DEVELOPER_ID") as String? ?: "")
                        name.set(project.findProperty("POM_DEVELOPER_NAME") as String? ?: "")
                        url.set(project.findProperty("POM_DEVELOPER_URL") as String? ?: "")
                    }
                }

                scm {
                    url.set(project.findProperty("POM_SCM_URL") as String? ?: "")
                    connection.set(project.findProperty("POM_SCM_CONNECTION") as String? ?: "")
                    developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String? ?: "")
                }
            }
        }
    }
}
