package io.github.kang30984.minisso

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfig {
    @Bean
    @ServiceConnection
    fun mysql(): MySQLContainer<Nothing> = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.4"))

    @Bean
    @ServiceConnection(name = "redis")
    fun redis(): GenericContainer<Nothing> =
        GenericContainer<Nothing>(DockerImageName.parse("redis:7.4-alpine")).apply {
            withExposedPorts(6379)
        }
}
