// mini-sso 리소스 서버 SDK. 의존성 한 줄과 설정 몇 줄로 토큰 검증, 사용자 주입, 권한 검사를 제공한다.
plugins {
    id("minisso.java-library-conventions")
}

dependencies {
    api(project(":auth-sdk:auth-sdk-core"))
    // 토큰 인증 필터와 예외 처리는 Spring Security 리소스 서버를 그대로 쓴다
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // 웹 MVC는 사용하는 서비스가 가져온다 (SDK가 웹 스택을 강요하지 않음)
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
}
