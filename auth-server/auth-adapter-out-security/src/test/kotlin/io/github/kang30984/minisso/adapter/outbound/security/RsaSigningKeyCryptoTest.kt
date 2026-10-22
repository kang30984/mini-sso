package io.github.kang30984.minisso.adapter.outbound.security

import com.nimbusds.jose.jwk.JWK
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import javax.crypto.AEADBadTagException

class RsaSigningKeyCryptoTest {

    private val crypto = RsaSigningKeyCrypto(TEST_KEY)

    @Test
    fun `공개 JWK에는 개인키가 없고, 복호화한 JWK에는 개인키가 있다`() {
        val generated = crypto.generate()

        val publicJwk = JWK.parse(generated.publicJwk)
        val privateJwk = JWK.parse(crypto.decryptPrivateJwk(generated.kid, generated.encryptedPrivateJwk))

        assertThat(publicJwk.isPrivate).isFalse()
        assertThat(privateJwk.isPrivate).isTrue()
        assertThat(privateJwk.keyID).isEqualTo(generated.kid).isEqualTo(publicJwk.keyID)
        assertThat(generated.encryptedPrivateJwk).doesNotContain("\"d\"")
    }

    @Test
    fun `다른 kid로 복호화하면 실패한다 (암호문 바꿔치기 방지)`() {
        val generated = crypto.generate()

        assertThatThrownBy { crypto.decryptPrivateJwk("other-kid", generated.encryptedPrivateJwk) }
            .isInstanceOf(AEADBadTagException::class.java)
    }

    @Test
    fun `다른 마스터 키로는 복호화할 수 없다`() {
        val generated = crypto.generate()
        val otherCrypto = RsaSigningKeyCrypto("YBCmeqi35jxOgzqWeTVmE/0QpQLTGZn2J9uTsqbdDhE=")

        assertThatThrownBy { otherCrypto.decryptPrivateJwk(generated.kid, generated.encryptedPrivateJwk) }
            .isInstanceOf(AEADBadTagException::class.java)
    }

    @Test
    fun `마스터 키가 없거나 길이가 맞지 않으면 기동을 막는다`() {
        assertThatThrownBy { RsaSigningKeyCrypto("") }.isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { RsaSigningKeyCrypto("c2hvcnQ=") }.isInstanceOf(IllegalStateException::class.java)
    }

    companion object {
        private const val TEST_KEY = "KotKMvmve6ddAuIG68bfUKPIjCR6PW2mK52LOqFAneA="
    }
}
