import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 인증 서버 모듈 공통 규약 (Kotlin)
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "io.github.kang30984.minisso"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
