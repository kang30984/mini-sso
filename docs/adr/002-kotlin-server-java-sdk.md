# ADR-002 인증 서버는 Kotlin, SDK는 Java로 작성

## 배경
SDK는 여러 서비스가 의존한다. 사용하는 서비스 중에는 Java 전용 프로젝트도 있다고 가정한다.

## 선택지
1. 전부 Kotlin
2. 전부 Java
3. 서버는 Kotlin, SDK는 Java

## 결정
3번.

## 결과
- 서버: null 안정성, data class, sealed class(로그인 결과 표현 등)로 도메인 규칙을 간결하고 안전하게 표현한다.
- SDK: 사용하는 서비스에 `kotlin-stdlib` 전이 의존을 강요하지 않는다. Java 서비스 입장에서 Kotlin 특유의 API(Companion, 기본 인자, `Unit` 반환 등)를 신경 쓸 필요가 없다.
- 공개 API에 `record`와 `Optional`을 사용해 Kotlin 서비스에서도 자연스럽게 쓰이도록 한다.
- 두 언어의 빌드 규약을 각각 유지해야 한다 (`minisso.kotlin-conventions`, `minisso.java-library-conventions`).
