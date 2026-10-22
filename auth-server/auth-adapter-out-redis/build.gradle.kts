plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // 로그인(SSO) 세션을 Redis에 저장해 여러 인스턴스가 세션을 공유하고 재시작해도 유지되게 한다
    implementation("org.springframework.session:spring-session-data-redis")
    // OAuth2AuthorizationService 구현 (DESIGN 4.2)
    implementation("org.springframework.security:spring-security-oauth2-authorization-server")
}
