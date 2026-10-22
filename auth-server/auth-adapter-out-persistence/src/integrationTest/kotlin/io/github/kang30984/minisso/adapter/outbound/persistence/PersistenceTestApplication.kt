package io.github.kang30984.minisso.adapter.outbound.persistence

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/** 영속성 어댑터만 단독으로 띄우기 위한 테스트 전용 부트 설정 */
@SpringBootApplication
class PersistenceTestApplication

@TestConfiguration(proxyBeanMethods = false)
class MySqlContainerConfig {
    @Bean
    @ServiceConnection
    fun mysql(): MySQLContainer<Nothing> = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.4"))
}
