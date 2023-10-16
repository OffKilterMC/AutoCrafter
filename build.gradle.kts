plugins {
    id("fabric-loom") version "1.4-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
}

val modVersion = properties["mod_version"] as String
val minecraftVersion = properties["minecraft_version"] as String
val loaderVersion = properties["loader_version"] as String
val mavenGroup = properties["maven_group"] as String
val fabricVersion = properties["fabric_version"] as String
val archivesBaseName = properties["archives_base_name"] as String

version = modVersion
group = mavenGroup

repositories {
    mavenCentral()
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")

        // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.8.3+kotlin.1.7.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
}

tasks.named<Copy>("processResources") {
    val props = mapOf("version" to version,
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion)

    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

val targetJavaVersion = 17

tasks.named<JavaCompile>("compileJava") {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    from("LICENSE") {
        rename { "${it}_${archivesBaseName}"}
    }
}
