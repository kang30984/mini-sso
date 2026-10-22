pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mini-sso"

include(
    ":auth-server:auth-domain",
    ":auth-server:auth-application",
    ":auth-server:auth-adapter-in-web",
    ":auth-server:auth-adapter-in-oauth2",
    ":auth-server:auth-adapter-out-persistence",
    ":auth-server:auth-adapter-out-redis",
    ":auth-server:auth-adapter-out-notifier",
    ":auth-server:auth-adapter-out-security",
    ":auth-server:auth-bootstrap",
    ":auth-sdk:auth-sdk-core",
    ":auth-sdk:auth-sdk-spring-boot-starter",
)

// sample-app-a: 로그인하는 웹 서비스 (Java, Spring Security OAuth2 Client)
// sample-app-b: SDK로 보호되는 API 서비스 (Kotlin)
include(":samples:sample-app-a", ":samples:sample-app-b")
