pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "JARVISLocalV2"
include(":app")
if (providers.gradleProperty("includeP2Fixture").orNull == "true") {
    include(":p2-client")
    project(":p2-client").projectDir = file("fixtures/p2-client")
}
