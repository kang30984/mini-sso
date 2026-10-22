import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 샘플 서비스(Java Spring Boot 앱) 규약. SDK를 사용하는 쪽이 Java 서비스일 수 있음을 보여주기 위해 Java로 작성한다.
plugins {
    java
    id("org.springframework.boot")
}

group = "io.github.kang30984.minisso"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
