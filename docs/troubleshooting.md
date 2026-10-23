# 트러블슈팅 기록

CI와 부하 테스트에서 실제로 실패했던 문제와 원인, 해결 방법을 정리했다. 모두 "로컬에서 우연히 통과하던 것"이 모듈 분리나 엄격한 테스트로 드러난 경우다.

## 1. 사용자 정의 테스트 스위트가 모듈 의존성을 못 봄

- **증상**: `integrationTest` 컴파일 시 같은 모듈의 도메인·애플리케이션 클래스를 찾지 못함
- **원인**: Gradle의 기본 `test`는 `implementation` 의존성을 상속하지만, `JvmTestSuite`로 만든 스위트는 상속하지 않는다
- **해결**: Convention Plugin에서 `integrationTestImplementation`이 `testImplementation`을 상속하도록 설정 (`minisso.integration-test-conventions`)
- **교훈**: 공통 빌드 설정을 Convention Plugin 한 곳에 모아 두어, 한 번 고치면 모든 모듈에 반영됐다

## 2. 앱 전체에서는 되는데 모듈 단독 테스트에서만 실패 (kotlin-reflect)

- **증상**: 영속성 모듈 통합 테스트에서 Spring Data가 `NoClassDefFoundError`
- **원인**: Spring Data는 Kotlin 엔티티 생성자를 분석할 때 `kotlin-reflect`가 필요하다. 앱 전체를 띄우면 웹 모듈의 `jackson-module-kotlin`이 이를 우연히 가져와 문제가 가려져 있었다
- **해결**: JPA 모듈 규약에 `kotlin-reflect`를 명시
- **교훈**: 모듈을 단독으로 테스트하면 숨은 전이 의존성이 드러난다. 멀티모듈의 효용을 확인한 사례

## 3. Kotlin 주석 안의 `/**`

- **증상**: `Syntax error: Unclosed comment`
- **원인**: Kotlin은 주석 중첩을 허용한다. KDoc에 경로를 `/api/admin/**`라고 적자 `/**`가 새 주석의 시작으로 해석됐다 (Java에서는 문제없는 표현)
- **해결**: 주석 문구 변경, 전체 소스에서 같은 패턴 검색

## 4. 테스트가 엉뚱한 이유로 통과하고 있었음 (인가 요청 파라미터)

- **증상**: 인가 코드 흐름 테스트 일부가 실패하고, 실패하지 않은 테스트도 의심스러움
- **원인**: Spring Authorization Server는 GET 인가 요청의 파라미터를 **쿼리 스트링에서만** 읽는다. MockMvc `param()`은 서블릿 파라미터로만 넣어 서버가 "파라미터 없는 요청(400)"으로 처리했다. 그 결과 "PKCE 누락 시 거부", "미등록 redirect_uri 거부" 테스트는 기대한 이유가 아니라 파라미터 누락으로 400이 나서 통과하고 있었다
- **해결**: 쿼리 스트링이 포함된 URI로 요청하도록 수정하고, PKCE 누락 테스트의 기대값을 "400 또는 error"에서 "302 + `error=invalid_request` + 코드 없음"으로 좁힘
- **교훈**: 실패 조건을 느슨하게 쓰면 잘못된 이유로 통과하는 테스트가 생긴다. 보안 테스트일수록 기대값을 정확하게 쓴다

## 5. 규격 권장 사항과 생태계 호환성의 충돌 (Logout Token `typ`)

- **증상**: Back-Channel Logout Token 검증 테스트에서 `BadJOSEException`
- **원인**: OIDC Back-Channel Logout 규격은 `typ: logout+jwt` 헤더를 권장(SHOULD)한다. 그러나 Nimbus 기반 디코더(Spring Security 포함)는 기본 설정에서 `typ`이 `JWT`이거나 없을 때만 허용한다
- **선택지**: 테스트 디코더만 설정을 바꿔 통과시키기 / 토큰에서 `typ` 제거
- **해결**: `typ`을 넣지 않음. 테스트만 고치면 실제 서비스(Spring Security 클라이언트)가 로그아웃 토큰을 거부하게 된다. 권장 사항이므로 생략해도 규격 위반은 아니며, Logout Token임은 필수 클레임 `events`로 구분된다
- **교훈**: 테스트를 통과시키는 것이 목적이 아니라, 테스트가 드러낸 실제 연동 문제를 고치는 것이 목적이다

## 6. Java 불변 컬렉션의 `contains(null)`

- **증상**: SDK 검증기를 만들 때 `NullPointerException` (`ImmutableCollections`)
- **원인**: 설정 객체가 audience 목록을 `Set.copyOf`로 불변 보관했고, 이를 Nimbus 클레임 검증기에 그대로 넘겼다. Nimbus는 내부에서 `contains(null)`을 호출하는데, `Set.of`/`Set.copyOf`로 만든 불변 Set은 이때 `false`가 아니라 NPE를 던진다
- **해결**: 외부 라이브러리에 넘길 때만 `HashSet`으로 복사
- **교훈**: 불변 컬렉션은 내부 보관용으로는 안전하지만, 외부 API에 넘길 때는 그 API의 사용 방식(여기서는 null 조회)을 확인해야 한다

## 7. BOM이 관리하지 않는 라이브러리

- **증상**: `Could not find com.nimbusds:nimbus-jose-jwt:.` (버전 비어 있음)
- **원인**: Spring Boot BOM이 관리할 것으로 가정하고 버전 없이 선언했다. 다른 모듈은 Spring Security가 버전과 함께 가져와 문제가 없었다
- **해결**: 버전 카탈로그에 버전을 명시. Spring Security가 더 높은 버전을 가져오면 Gradle 충돌 해결로 높은 쪽이 선택된다

## 8. 부하 테스트에서 드러난 동시 로그인 충돌 (500 오류)

- **증상**: 첫 부하 테스트에서 로그인 흐름 요청의 26%가 실패. 서버 로그에 `ObjectOptimisticLockingFailureException` 1,447건, 모두 같은 계정 엔티티
- **원인**: 로그인 성공 시 `lastLoginAt`을 갱신하는데 계정 엔티티에 `@Version`이 있어, 같은 계정의 동시 로그인끼리 충돌했다. 이 예외가 인증 실패로 처리되지 않고 500으로 노출됐다
- **해결**: 충돌 원인인 `lastLoginAt`·`updatedAt`을 낙관적 락 대상에서 제외하고, 보안 규칙인 실패 횟수 충돌은 트랜잭션 밖에서 1회 재시도. 회귀 테스트 `ConcurrentLoginIT` 추가 ([ADR-011](adr/011-concurrent-login-optimistic-lock.md))
- **시나리오도 수정**: 모든 가상 사용자가 계정 하나로 로그인하던 것을 계정 100개로 나눴다. 실제와 다른 경합이 측정을 왜곡하기 때문이다
- **교훈**: 단위·통합 테스트는 순차 실행이라 동시성 문제를 잡지 못했다. 부하 테스트가 성능 측정 도구이면서 동시성 버그 탐지 도구 역할을 했다
