pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "jarvis-core-integration-lab"
for ((name, property) in listOf("nexus" to "coreNexusSource", "services" to "coreServicesSource")) {
    val source = providers.gradleProperty(property).orNull ?: error("Explicit verified dependency required: $property")
    check(ProcessBuilder("python3", file("../tools/prepare-core03-sources.py").absolutePath, "--verify", name, source).inheritIO().start().waitFor() == 0) { "Dependency provenance refused" }
    includeBuild(source)
}
