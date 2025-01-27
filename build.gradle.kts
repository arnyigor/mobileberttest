// Top-level build file where you can add configuration options common to all sub-projects/modules.
// build.gradle.kts (на уровне проекта)

plugins {
    // Применяем плагин Kotlin DSL к корневому проекту
    kotlin("jvm") version "1.8.10" apply false
    id("com.android.application") version "8.1.0" apply false
}
