/**
 * Configuration for detekt static analysis
 */

package com.saveourtool.save.buildutils

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Configure Detekt for a single project
 */
fun Project.configureDetekt() {
    apply<DetektPlugin>()
    configure<DetektExtension> {
        config = rootProject.files("detekt.yml")
        basePath = rootDir.canonicalPath
        buildUponDefaultConfig = true
    }
    tasks.withType<Detekt>().configureEach {
        reports.sarif.required.set(true)
    }
}

/**
 * Register a unified detekt task
 */
fun Project.createDetektTask() {
    tasks.register("detektAll") {
        allprojects {
            this@register.dependsOn(tasks.withType<Detekt>())
        }
    }
}
