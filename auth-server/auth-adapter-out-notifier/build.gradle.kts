plugins {
    id("minisso.integration-test-conventions")
}

dependencies {
    implementation(project(":auth-server:auth-application"))
    // 4주차: Back-Channel Logout 발송용 RestClient (spring-web)
}
