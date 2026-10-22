package io.github.kang30984.minisso

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MiniSsoApplication

fun main(args: Array<String>) {
    runApplication<MiniSsoApplication>(*args)
}
