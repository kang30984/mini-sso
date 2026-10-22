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
    ":auth-server:auth-bootstrap",
    ":auth-sdk:auth-sdk-core",
    ":auth-sdk:auth-sdk-spring-boot-starter",
)

// 5주차: SDK 완성 후 샘플 앱 추가
// include(":samples:sample-app-a", ":samples:sample-app-b")
