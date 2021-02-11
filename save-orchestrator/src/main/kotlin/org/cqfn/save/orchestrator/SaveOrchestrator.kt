package org.cqfn.save.backend

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
@EnableWebFlux
open class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
