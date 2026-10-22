# ADR-003 OAuth2 서버 직접 구현 대신 Spring Authorization Server 채택

## 배경
OAuth2/OIDC는 표준 준수 범위가 넓고, 사소한 구현 실수가 곧 보안 취약점이 된다 (redirect_uri 검증, PKCE, 토큰 바인딩 등).

## 선택지
1. 직접 구현
2. Keycloak 등 외부 IdP 사용
3. Spring Authorization Server(SAS) 기반 구현

## 결정
3번. 프로토콜 처리는 검증된 구현에 맡기고, 계정 도메인과 정책은 직접 소유한다.

## 결과
- 외부 IdP(2번)는 운영은 편하지만, 계정 도메인 규칙과 저장 구조를 직접 설계하는 이 프로젝트의 목적과 맞지 않는다.
- SAS의 확장 지점(`RegisteredClientRepository`, `OAuth2AuthorizationService`, `JWKSource`, `OAuth2TokenCustomizer`)을 우리 포트를 호출하는 어댑터로 구현해 프레임워크가 도메인을 침범하지 않게 한다 (DESIGN.md 4.2).
- SAS 기본 기능 밖의 요구(Refresh Token 재사용 탐지 시 계보 폐기, Back-Channel Logout 발송)는 직접 구현한다.
- SAS 버전 업그레이드 시 확장 지점 시그니처 변경 영향을 받는다 → 어댑터 모듈에 영향 범위가 갇힌다.
