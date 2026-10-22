// Spring에 의존하지 않는 토큰 검증 라이브러리. 배치, 게이트웨이 등 Spring 밖에서도 쓸 수 있다.
plugins {
    id("minisso.java-library-conventions")
}

dependencies {
    api(libs.nimbus.jose.jwt)
}
