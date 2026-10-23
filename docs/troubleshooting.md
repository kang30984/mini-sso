# 트러블슈팅 기록

CI와 부하 테스트에서 실제로 막혔던 문제들이다. 돌아보면 대부분 "로컬이나 특정 조건에서는 우연히 통과하던 것"이 모듈을 나누거나 테스트를 엄격하게 바꾸면서 드러난 경우였다.

## 1. 직접 만든 테스트 스위트에서 모듈 의존성이 안 보임

통합 테스트용으로 `integrationTest` 스위트를 따로 만들었는데, 컴파일할 때 같은 모듈의 도메인·애플리케이션 클래스를 찾지 못했다.

Gradle의 기본 `test`는 `implementation` 의존성을 물려받지만, `JvmTestSuite`로 새로 만든 스위트는 물려받지 않는다. 그래서 Convention Plugin에서 `integrationTestImplementation`이 `testImplementation`을 상속하도록 설정했다(`minisso.integration-test-conventions`).

공통 빌드 설정을 Convention Plugin 한곳에 모아 둔 덕분에, 한 번 고치니 모든 모듈에 같이 반영됐다.

## 2. 앱 전체로는 되는데 모듈 단독 테스트에서만 실패 (kotlin-reflect)

영속성 모듈의 통합 테스트에서 Spring Data가 `NoClassDefFoundError`를 냈다. 이상한 건 앱 전체를 띄우는 테스트에서는 같은 코드가 잘 돌았다는 점이다.

Spring Data는 Kotlin 엔티티의 생성자를 분석할 때 `kotlin-reflect`를 쓴다. 앱 전체를 띄우면 웹 모듈의 `jackson-module-kotlin`이 이걸 같이 가져와서 문제가 가려져 있었고, 영속성 모듈만 따로 띄우니 비로소 드러났다. JPA 모듈 규약에 `kotlin-reflect`를 명시해서 해결했다.

모듈을 나눠서 따로 테스트하지 않았다면, 나중에 웹 모듈 의존성을 정리하다가 엉뚱한 곳이 깨졌을 것이다.

## 3. Kotlin 주석 안의 `/**`

`Syntax error: Unclosed comment`라는 컴파일 오류가 났다. KDoc 주석에 경로를 `/api/admin/**`라고 적어 둔 게 원인이었다. Kotlin은 주석 안에 주석을 중첩할 수 있어서, 주석 속의 `/**`를 새 주석의 시작으로 읽었다. Java에서는 아무 문제 없는 표현이다.

주석 문구를 바꾸고, 전체 소스에서 같은 패턴이 있는지 검색해 봤다.

## 4. 테스트가 엉뚱한 이유로 통과하고 있었음

인가 코드 흐름 테스트 일부가 실패해서 원인을 찾다 보니, 통과하던 테스트도 믿을 수 없는 상태였다.

Spring Authorization Server는 GET 인가 요청의 파라미터를 쿼리 스트링에서만 읽는다. 그런데 MockMvc의 `param()`은 값을 서블릿 파라미터로만 넣고 쿼리 스트링에는 넣지 않는다. 서버 입장에서는 파라미터가 하나도 없는 요청이라 400을 돌려줬다.

그 결과 "PKCE 없이 요청하면 거부한다", "등록되지 않은 redirect_uri는 거부한다" 테스트가 기대한 이유가 아니라 그냥 파라미터가 없어서 400이 나는 바람에 통과하고 있었다.

요청을 쿼리 스트링이 들어간 URI로 보내도록 고쳤다. 그리고 PKCE 누락 테스트의 기대값도 "400이거나 error가 있으면 통과"에서 "302로 돌아가고, `error=invalid_request`가 있고, 코드는 없어야 통과"로 좁혔다.

실패 조건을 느슨하게 쓰면 잘못된 이유로 통과하는 테스트가 생긴다. 보안 관련 테스트일수록 기대값을 정확하게 써야 한다는 걸 이때 느꼈다.

## 5. 규격대로 했더니 라이브러리가 토큰을 거부함 (Logout Token `typ`)

Back-Channel Logout 토큰을 검증하는 테스트에서 `BadJOSEException`이 났다.

OIDC Back-Channel Logout 규격은 토큰 헤더에 `typ: logout+jwt`를 넣도록 권장(SHOULD)한다. 그런데 Nimbus 기반 디코더는 기본 설정에서 `typ`이 `JWT`이거나 아예 없을 때만 받아들인다. Spring Security도 이 디코더를 쓴다.

테스트용 디코더 설정만 바꾸면 테스트는 통과시킬 수 있었다. 하지만 그러면 실제로 토큰을 받는 서비스(Spring Security 클라이언트)는 그대로 거부하게 된다. 그래서 토큰에서 `typ`을 빼는 쪽을 택했다. 권장 사항이라 빼도 규격 위반은 아니고, Logout Token인지는 필수 클레임인 `events`로 구분할 수 있다.

## 6. Java 불변 컬렉션의 `contains(null)`

SDK 검증기를 만드는 순간 `NullPointerException`이 났다. 스택에는 `ImmutableCollections`가 찍혀 있었다.

설정 객체가 허용할 audience 목록을 `Set.copyOf`로 불변 Set으로 들고 있었고, 그걸 Nimbus의 클레임 검증기에 그대로 넘겼다. Nimbus는 내부에서 `contains(null)`을 호출하는데, `Set.of`나 `Set.copyOf`로 만든 불변 Set은 이때 `false`를 돌려주지 않고 NPE를 던진다. 일반 `HashSet`과 다르게 동작하는 부분이라 미처 몰랐다.

외부 라이브러리에 넘길 때만 `HashSet`으로 복사해서 넘기도록 고쳤다. 불변 컬렉션은 안에서 들고 있기에는 안전하지만, 밖으로 넘길 때는 받는 쪽이 어떻게 쓰는지 확인해야 한다.

## 7. BOM이 관리하지 않는 라이브러리

`Could not find com.nimbusds:nimbus-jose-jwt:.`라는 오류가 났다. 콜론 뒤가 비어 있는 게 버전이 없다는 뜻이다.

Spring Boot BOM이 버전을 관리해 줄 거라고 생각하고 버전 없이 선언했는데, 이 라이브러리는 BOM 관리 대상이 아니었다. 다른 모듈에서는 Spring Security가 버전과 함께 가져오니까 문제가 없었고, 직접 선언한 두 모듈에서만 터졌다. 버전 카탈로그에 버전을 명시해서 해결했다. Spring Security가 더 높은 버전을 가져오면 Gradle이 알아서 높은 쪽으로 맞춘다.

## 8. 부하 테스트에서 드러난 동시 로그인 충돌

첫 부하 테스트에서 로그인 요청의 26%가 실패했다. 서버 로그에 `ObjectOptimisticLockingFailureException`이 1,447건 있었고, 전부 같은 계정 엔티티였다.

로그인에 성공할 때마다 `lastLoginAt`을 갱신하는데, 계정 엔티티에 `@Version`이 걸려 있어서 같은 계정의 동시 로그인끼리 충돌했다. 이 예외가 인증 실패로 처리되지 않고 500으로 나가고 있었다.

충돌 원인인 `lastLoginAt`과 `updatedAt`은 낙관적 락 대상에서 빼고, 실패 횟수처럼 보안에 필요한 값의 충돌만 트랜잭션 바깥에서 한 번 재시도하게 했다. 같은 상황을 재현하는 `ConcurrentLoginIT`도 추가했다 ([ADR-011](adr/011-concurrent-login-optimistic-lock.md)).

부하 테스트 시나리오도 같이 고쳤다. 가상 사용자 전원이 계정 하나로 로그인하던 걸 계정 100개로 나눴다. 실제로는 없는 경합이 측정 결과를 흐리기 때문이다.

단위 테스트와 통합 테스트는 순서대로 하나씩 돌기 때문에 이런 동시성 문제를 잡지 못했다. 성능을 재려고 돌린 부하 테스트가 결과적으로 동시성 버그를 찾아 준 셈이다.

## 9. 최신 Docker Desktop에서 통합 테스트가 Docker를 찾지 못함

Windows에서 최신 Docker Desktop으로 `gradlew build`를 돌렸더니 통합 테스트가 테스트 메서드를 하나도 실행하지 못하고 `initializationError`로 실패했다. 원인은 `IllegalStateException: Could not find a valid Docker environment`였다. `docker compose up -d`는 잘 되는데 Testcontainers만 Docker에 붙지 못했다.

Docker Engine 29부터 데몬이 받아 주는 최소 API 버전이 1.44로 올라갔는데, Spring Boot 3.5.6 BOM이 가져오는 Testcontainers 1.21.3은 그보다 낮은 버전(1.32)으로 요청을 보낸다. 데몬이 요청을 거부하니 Testcontainers는 "쓸 수 있는 Docker 환경이 없다"고 판단한다. 그 뒤에 나오는 `lateinit property connectionFactory has not been initialized`는 컨테이너가 안 뜬 상태에서 정리 메서드가 돌면서 생긴 부수적인 오류다. 진짜 원인은 테스트 리포트의 Standard output 탭에 있는 `client version 1.32 is too old` 메시지에서 확인할 수 있다.

Testcontainers 1.21.4가 최신 Docker Engine을 지원하므로, 버전 카탈로그에 버전을 명시하고 통합 테스트 규약에서 `testcontainers-bom`을 플랫폼으로 추가했다. Spring Boot BOM의 1.21.3과 겹치지만 Gradle이 높은 쪽을 고른다. Spring Boot를 Testcontainers 1.21.4 이상을 관리하는 버전으로 올리면 이 설정은 지워도 된다.

코드를 바꿀 수 없는 상황이라면 사용자 홈 디렉터리의 `.docker-java.properties`에 `api.version=1.44`를 넣어 요청 버전을 강제하는 방법도 있다. 다만 이건 PC마다 따로 해 줘야 해서 근본적인 해결은 아니다.
