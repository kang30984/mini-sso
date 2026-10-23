plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    // 트랜잭션 밖에서 낙관적 락 충돌을 재시도하기 위해 예외 타입을 참조한다
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}
