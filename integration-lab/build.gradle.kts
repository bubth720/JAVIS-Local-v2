plugins { kotlin("jvm") version "2.0.21"; application }
kotlin { jvmToolchain(17) }
dependencies {
    implementation("fr.jarvis.nexus:nexus-core:0.1.0")
    implementation("fr.jarvis.services:shared-runtime:1.0.0")
    implementation("fr.jarvis.services:desktop:1.0.0")
    testImplementation("junit:junit:4.13.2")
}
application { mainClass.set("com.stark.jarvislocal.integrations.CoreIntegrationLabKt") }
tasks.test { useJUnit() }
