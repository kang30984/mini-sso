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
                implementation("org.assertj:assertj-core")
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
