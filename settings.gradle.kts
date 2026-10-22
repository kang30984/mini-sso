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

// sample-app-a: 3주차 (Spring Security OAuth2 Client로 로그인)
// sample-app-b: 5주차 (SDK 적용 후 추가)
include(":samples:sample-app-a")
