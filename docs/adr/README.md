# 설계 결정 기록 (ADR)

구현하면서 고민이 필요했던 결정들을 남겨 둔 문서다. 각 문서에는 왜 그 문제를 고민했는지, 어떤 방법들을 비교했는지, 무엇을 골랐고 그 대가가 무엇인지를 적었다.

| 번호 | 제목 |
|---|---|
| [001](001-hexagonal-multi-module.md) | 헥사고날 구조를 Gradle 모듈로 나누기 |
| [002](002-kotlin-server-java-sdk.md) | 인증 서버는 Kotlin, SDK는 Java |
| [003](003-spring-authorization-server.md) | OAuth2 서버를 직접 만들지 않고 Spring Authorization Server 쓰기 |
| [004](004-authorization-state-in-redis.md) | 인가 상태와 로그인 세션을 Redis에 저장 |
| [005](005-local-jwt-verification.md) | Introspection 대신 JWT를 서비스에서 직접 검증 |
| [006](006-transactional-in-application.md) | application 계층에 @Transactional만 허용 |
| [007](007-refresh-token-reuse-detection.md) | 교체된 리프레시 토큰이 다시 오면 전부 폐기 |
| [008](008-signing-key-rotation.md) | 서명 키 무중단 교체 |
| [009](009-login-history-without-fk.md) | 로그인 이력 테이블에 FK를 두지 않기 |
| [010](010-first-party-clients-pkce.md) | 동의 화면 생략, PKCE는 모든 클라이언트에 강제 |
| [011](011-concurrent-login-optimistic-lock.md) | 같은 계정의 동시 로그인과 낙관적 락 |
| [012](012-verified-client-secret-cache.md) | 검증된 클라이언트 시크릿 캐시 |
