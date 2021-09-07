import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    val shadowVersion: String by System.getProperties()
    id("com.github.johnrengelman.shadow").version(shadowVersion)
}

val pluginGroup: String by project
group = pluginGroup
val pluginVersion: String by project
version = pluginVersion

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    val kotlinVersion: String by System.getProperties()
    implementation(kotlin("stdlib", kotlinVersion))
    val paperAPIVersion: String by project
    compileOnly("io.papermc.paper", "paper-api", paperAPIVersion)
}

val autoRelocate by tasks.register<ConfigureShadowRelocation>("configureShadowRelocation", ConfigureShadowRelocation::class) {
    target = tasks.getByName("shadowJar") as ShadowJar?
    val packageName = "${project.group}.${project.name.toLowerCase()}"
    prefix = "$packageName.shaded"
}

tasks {
    val javaVersion = JavaVersion.VERSION_16
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    val testServerPath: String by project
    val cleanPlugins by register("cleanPlugins") {
        delete("$testServerPath/plugins")
    }
    val copyPluginToServer by register<Copy>("copyPluginToServer") {
        dependsOn(shadowJar)
        from("$buildDir/libs")
        into("$testServerPath/plugins")
    }
    shadowJar {
        archiveClassifier.set("")
        project.configurations.implementation.get().isCanBeResolved = true
        configurations = listOf(project.configurations.implementation.get())
        dependsOn(autoRelocate)
        minimize()
    }
    register<JavaExec>("deploy") {
        dependsOn(clean, cleanPlugins, copyPluginToServer)
        classpath("$testServerPath/paper-1.17.1.jar")
        workingDir = File(testServerPath)
        standardInput = System.`in`
        args = listOf("nogui")
    }
}
