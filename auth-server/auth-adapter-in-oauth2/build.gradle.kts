plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    // 3주차: org.springframework.boot:spring-boot-starter-oauth2-authorization-server
}
