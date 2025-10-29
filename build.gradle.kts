import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Versions are defined in settings.gradle.kts via pluginManagement
    id("com.github.ben-manes.versions") version "0.53.0"
}

allprojects {
    // Ensure Java toolchain (Java 17) is requested for all JavaCompile tasks — more robust than relying only on org.gradle.java.home
    tasks.withType<JavaCompile>().configureEach {
        // Use Gradle's JavaToolchainService to obtain a compiler for Java 17
        javaCompiler.set(
            project.extensions
                .getByType(org.gradle.jvm.toolchain.JavaToolchainService::class.java)
                .compilerFor { languageVersion.set(JavaLanguageVersion.of(17)) }
        )
    }

    // Common configuration can go here if needed
}
