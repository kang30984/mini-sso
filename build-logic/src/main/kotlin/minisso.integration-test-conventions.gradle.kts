import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Testcontainers 기반 통합 테스트를 단위 테스트와 분리 (src/integrationTest)
plugins {
    id("minisso.kotlin-conventions")
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(platform(SpringBootPlugin.BOM_COORDINATES))
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.testcontainers:junit-jupiter")
            }

            // 사용자 정의 테스트 스위트는 기본 test와 달리 모듈의 implementation 의존성을 물려받지 않는다.
            // 통합 테스트도 도메인·애플리케이션 클래스를 사용하므로 test 설정을 그대로 상속한다.
            configurations.named(sources.implementationConfigurationName) {
                extendsFrom(configurations.getByName("testImplementation"))
            }
            configurations.named(sources.runtimeOnlyConfigurationName) {
                extendsFrom(configurations.getByName("testRuntimeOnly"))
            }

            targets.all {
                testTask.configure {
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
