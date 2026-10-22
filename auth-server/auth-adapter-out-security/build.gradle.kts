// 비밀번호 해시 등 암호화 관련 기술 구현
plugins {
    id("minisso.kotlin-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-crypto")
    // 서명 키(RSA JWK) 생성
    implementation("com.nimbusds:nimbus-jose-jwt")
    // Argon2 구현체
    runtimeOnly(libs.bouncycastle.bcprov)
}
