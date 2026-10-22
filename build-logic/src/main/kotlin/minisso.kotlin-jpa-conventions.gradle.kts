// JPA 엔티티를 Kotlin으로 작성하는 모듈 규약
// - plugin.jpa: 엔티티에 기본 생성자 생성
// - allOpen: Hibernate 지연 로딩 프록시를 위해 엔티티 클래스를 open으로 컴파일
// - kapt: QueryDSL Q타입 생성
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
