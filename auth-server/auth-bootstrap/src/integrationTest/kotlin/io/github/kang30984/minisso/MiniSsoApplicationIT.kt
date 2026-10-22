package io.github.kang30984.minisso

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Import(MiniSsoApplicationIT.Containers::class)
class MiniSsoApplicationIT {

    @Test
    fun `MySQL과 Redis에 연결된 상태로 컨텍스트가 기동된다`() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Containers {
        @Bean
        @ServiceConnection
        fun mysql(): MySQLContainer<Nothing> =
            MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.4"))

        @Bean
        @ServiceConnection(name = "redis")
        fun redis(): GenericContainer<Nothing> =
            GenericContainer<Nothing>(DockerImageName.parse("redis:7.4-alpine")).apply {
                withExposedPorts(6379)
            }
    }
}
