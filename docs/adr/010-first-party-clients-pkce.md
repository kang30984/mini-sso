# ADR-010 1st-party 클라이언트는 동의 화면 생략, PKCE는 기밀 클라이언트에도 필수

## 배경
mini-sso에 등록되는 클라이언트는 모두 같은 그룹의 사내 서비스(1st-party)다. 또한 모든 클라이언트가 서버 사이드 앱이라 client_secret을 안전하게 보관할 수 있는 기밀(confidential) 클라이언트다.

## 선택지
1. 동의 화면: 표시 / 생략
2. PKCE: 공개 클라이언트에만 적용 / 모든 클라이언트에 적용

## 결정
- 동의 화면은 생략한다 (`requireAuthorizationConsent(false)`).
- PKCE는 모든 클라이언트에 필수로 적용하고, `S256`만 허용한다 (`requireProofKey(true)`).

## 결과
- 사용자는 "이 앱이 내 정보에 접근하려 합니다" 같은 화면 없이 한 번의 로그인으로 사내 서비스를 오간다. 3rd-party 클라이언트를 받게 되면 클라이언트별로 동의 화면을 켤 수 있도록 설정을 분리한다.
- 기밀 클라이언트도 PKCE를 쓰면, 인가 코드가 탈취되더라도(브라우저 기록, 로그, 리퍼러 등) code_verifier 없이는 토큰으로 바꿀 수 없다. OAuth 2.1 초안과 RFC 9700(OAuth 2.0 Security BCP)의 권고와도 일치한다.
- 클라이언트 쪽 부담이 생긴다. Spring Security OAuth2 Client는 기밀 클라이언트에 PKCE를 기본으로 붙이지 않으므로 `OAuth2AuthorizationRequestCustomizers.withPkce()`를 설정해야 한다 (sample-app-a 참고). 5주차 SDK가 이 설정을 대신해 준다.
