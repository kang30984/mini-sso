// JPA 엔티티를 Kotlin으로 작성하는 모듈 규약
// - plugin.jpa: 엔티티에 기본 생성자 생성
// - allOpen: Hibernate 지연 로딩 프록시를 위해 엔티티 클래스를 open으로 컴파일
// - kapt: QueryDSL Q타입 생성
// - kotlin-reflect: Spring Data가 Kotlin 엔티티의 생성자를 분석할 때 필요
//   (다른 모듈의 jackson-module-kotlin을 통해 우연히 채워지지 않도록 명시한다)
plugins {
    id("minisso.integration-test-conventions")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(kotlin("reflect"))
}
