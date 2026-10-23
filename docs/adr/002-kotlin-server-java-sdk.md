# ADR-002 인증 서버는 Kotlin, SDK는 Java

## 배경

SDK는 여러 서비스가 의존성으로 가져다 쓴다. 그중에는 Java로만 된 프로젝트도 있을 수 있다.

## 검토한 방법

1. 전부 Kotlin
2. 전부 Java
3. 서버는 Kotlin, SDK는 Java

## 결정

3번으로 했다.

## 결과

서버 쪽은 Kotlin 덕을 많이 봤다. null 안정성 덕분에 "있을 수도 없을 수도 있는 값"이 타입에 드러나고, 로그인 결과처럼 경우의 수가 정해진 값은 sealed class로 표현하니 `when`에서 빠진 경우를 컴파일러가 잡아 준다.

SDK를 Java로 쓴 이유는 단순하다. Kotlin으로 쓰면 SDK를 쓰는 모든 서비스에 kotlin-stdlib가 딸려 들어가고, Java 서비스 개발자가 companion object나 기본 인자 같은 Kotlin 문법을 신경 써야 한다. 공개 API는 Java `record`로 만들어서 Kotlin 서비스에서도 어색하지 않게 쓸 수 있다. 실제로 Kotlin으로 만든 샘플 앱 B에서 그대로 쓰고 있다.

대신 빌드 규약을 언어별로 두 벌(`minisso.kotlin-conventions`, `minisso.java-library-conventions`) 관리해야 한다.
