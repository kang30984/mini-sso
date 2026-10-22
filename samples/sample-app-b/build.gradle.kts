// SDK만으로 보호되는 API 서비스 (Kotlin). 보안 설정 코드 없이 의존성과 설정 몇 줄로 동작한다.
plugins {
    id("minisso.spring-boot-app-conventions")
}

dependencies {
    implementation(project(":auth-sdk:auth-sdk-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
