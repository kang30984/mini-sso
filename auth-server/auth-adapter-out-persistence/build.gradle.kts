plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // 2주차: QueryDSL (jakarta classifier) + kapt
}
