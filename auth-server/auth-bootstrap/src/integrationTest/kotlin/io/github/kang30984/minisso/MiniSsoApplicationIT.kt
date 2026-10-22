package io.github.kang30984.minisso

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestContainersConfig::class)
class MiniSsoApplicationIT {

    @Test
    fun `MySQL과 Redis에 연결되고 Flyway 스키마와 엔티티가 일치한 상태로 기동된다`() {
    }
}
