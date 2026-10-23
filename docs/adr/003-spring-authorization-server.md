# ADR-003 OAuth2 서버를 직접 만들지 않고 Spring Authorization Server 쓰기

## 배경

OAuth2와 OIDC는 규격 범위가 넓고, redirect_uri 검증이나 PKCE 같은 부분에서 작은 실수 하나가 그대로 보안 취약점이 된다.

## 검토한 방법

1. 직접 구현
2. Keycloak 같은 외부 IdP 사용
3. Spring Authorization Server 위에 구현

## 결정

3번으로 했다. 프로토콜 처리는 검증된 구현에 맡기고, 계정 도메인과 정책은 직접 가져간다.

## 결과

Keycloak을 쓰면 운영은 편하지만, 계정 규칙과 저장 구조를 직접 설계해 보려는 이 프로젝트의 목적과는 맞지 않았다.

Spring Authorization Server는 확장 지점(`RegisteredClientRepository`, `OAuth2AuthorizationService`, `OAuth2TokenCustomizer` 등)을 인터페이스로 열어 두고 있다. 이것들을 애플리케이션 포트를 호출하는 어댑터로 구현해서, 프레임워크 코드가 도메인 안으로 들어오지 않게 했다.

프레임워크가 해 주지 않는 부분은 직접 만들었다. 리프레시 토큰 재사용을 탐지했을 때 토큰을 전부 폐기하는 처리와, Back-Channel Logout 토큰을 보내는 부분이 그렇다.

나중에 Spring Authorization Server 버전을 올리다가 확장 지점의 시그니처가 바뀌어도, 고쳐야 할 곳은 어댑터 모듈 안으로 한정된다.
