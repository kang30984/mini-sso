plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    // Logout Token 서명 (인가 서버와 같은 키를 쓰는 JwtEncoder 빈을 주입받는다)
    implementation("org.springframework.security:spring-security-oauth2-jose")

    testImplementation("org.springframework:spring-test")
}
