plugins {
    id("java")
    id("dev.architectury.loom") version("1.7-SNAPSHOT")
    id("architectury-plugin") version("3.4-SNAPSHOT")
    kotlin("jvm") version ("1.8.10")
}

group = "com.github.kuramastone"
version = "1.0.2"

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()

    mixin {
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.fabricmc.net/")
}

val fabricApiVersion: String by project
val bUtilitiesVersion: String by project

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.12.3+kotlin.2.0.21")

    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-events-interaction-v0"
    ).forEach {
        // Add each module as a dependency
        modImplementation(fabricApi.module(it, "$fabricApiVersion"))
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    includeAndImplement("com.github.kuramastone:BUtilities-Core:$bUtilitiesVersion")
    includeAndImplement("net.kyori:adventure-api:4.17.0")
    includeAndImplement("net.kyori:examination-api:1.3.0")
    includeAndImplement("net.kyori:adventure-key:4.17.0")
    includeAndImplement("net.kyori:adventure-text-serializer-plain:4.14.0")

    //modImplementation("com.cobblemon:fabric:1.6.0+1.21-SNAPSHOT")
    modImplementation(files("libs/Cobblemon-fabric-1.6.0+1.21.1-main-7ae20cc.jar"))

    compileOnly("net.luckperms:api:5.4")
}

fun DependencyHandler.includeAndImplement(dependency: String) {
    include(dependency)
    implementation(dependency)
}

val targetJavaVersion = 21

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion.toString())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}