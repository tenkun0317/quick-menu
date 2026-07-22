plugins {
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("kapt")
    `maven-publish`
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.terraformersmc.com/", "Terraformers", "com.terraformersmc")
    maven("https://maven.wispforest.io")
    maven("https://jitpack.io")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.isxander.dev/releases/")
    maven("https://maven.nucleoid.xyz")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    val fabricApi: String = sc.properties["deps.fabric_api"]
    val fabricLanguageKotlin: String = sc.properties["deps.fabric_language_kotlin"]
    val yacl: String = sc.properties["deps.yacl"]
    val modMenu: String = sc.properties["deps.mod_menu"]
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApi}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${fabricLanguageKotlin}")
    modImplementation("maven.modrinth:1eAoo2KR:${yacl}")
    modImplementation("com.terraformersmc:modmenu:${modMenu}")
    implementation("com.google.code.gson:gson:${property("deps.gson")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

tasks.processResources {
    val props = buildMap<String, String> {
        fun register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            put(key, value)
        }
        register("id", "mod.id")
        register("name", "mod.name")
        register("version", "mod.version")
        register("minecraft", "mod.mc_compat")
    }

    filesMatching("fabric.mod.json") { expand(props) }

    val mixinJava = "JAVA_25"
    filesMatching("*.mixins.json") { expand("java" to mixinJava) }
}

val licenseFileForJar = rootProject.projectDir.resolve("LICENSE")
val archivesBaseNameForJar = base.archivesName.get()
tasks.jar {
    from(licenseFileForJar) {
        rename { "${it}_$archivesBaseNameForJar" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
