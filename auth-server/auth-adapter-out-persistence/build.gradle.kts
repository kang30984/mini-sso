plugins {
    id("minisso.kotlin-jpa-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-mysql")

    implementation(variantOf(libs.querydsl.jpa) { classifier("jakarta") })
    kapt(variantOf(libs.querydsl.apt) { classifier("jakarta") })
    kapt("jakarta.persistence:jakarta.persistence-api:3.1.0")
    kapt("jakarta.annotation:jakarta.annotation-api:2.1.1")
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation("org.testcontainers:mysql")
                runtimeOnly("com.mysql:mysql-connector-j")
            }
        }
    }
}
