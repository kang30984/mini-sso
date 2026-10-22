plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
