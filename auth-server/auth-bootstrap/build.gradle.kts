plugins {
    id("minisso.spring-boot-app-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation(project(":auth-server:auth-adapter-in-web"))
    implementation(project(":auth-server:auth-adapter-in-oauth2"))
    implementation(project(":auth-server:auth-adapter-out-persistence"))
    implementation(project(":auth-server:auth-adapter-out-redis"))
    implementation(project(":auth-server:auth-adapter-out-notifier"))
    implementation(project(":auth-server:auth-adapter-out-security"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("com.mysql:mysql-connector-j")

    // 아키텍처 규칙은 모든 모듈이 클래스패스에 있는 여기서 검증
    testImplementation(libs.archunit.junit5)
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation("org.testcontainers:mysql")
            }
        }
    }
}
