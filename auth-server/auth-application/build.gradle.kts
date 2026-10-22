plugins {
    id("minisso.kotlin-conventions")
}

dependencies {
    api(project(":auth-server:auth-domain"))

    // ADR-006: 트랜잭션 경계 선언(@Transactional)만 허용
    implementation("org.springframework:spring-tx")

    testImplementation(testFixtures(project(":auth-server:auth-domain")))
}
