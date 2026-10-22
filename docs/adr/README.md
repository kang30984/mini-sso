# 설계 결정 기록 (ADR)

각 문서는 배경 → 선택지 → 결정 → 결과(트레이드오프) 순서로 작성한다.

| 번호 | 제목 | 상태 |
|---|---|---|
| [001](001-hexagonal-multi-module.md) | 헥사고날 아키텍처와 멀티모듈 채택 | 승인 |
| [002](002-kotlin-server-java-sdk.md) | 인증 서버는 Kotlin, SDK는 Java | 승인 |
| [003](003-spring-authorization-server.md) | Spring Authorization Server 채택 | 승인 |
| [004](004-authorization-state-in-redis.md) | 인가 상태와 로그인 세션을 Redis에 저장 | 승인 |
| 005 | Introspection 대신 짧은 수명 JWT + 로컬 검증 | 5주차 작성 예정 |
| [006](006-transactional-in-application.md) | application 계층에 @Transactional 허용 | 승인 |
| [007](007-refresh-token-reuse-detection.md) | 회전된 리프레시 토큰이 재사용되면 인가 전체를 폐기 | 승인 |
| 008 | 서명키 로테이션 시 이전 키 게시 기간 | 5주차 작성 예정 |
| [009](009-login-history-without-fk.md) | 로그인 이력 테이블에 계정 FK를 두지 않음 | 승인 |
| [010](010-first-party-clients-pkce.md) | 1st-party 클라이언트는 동의 화면 생략, PKCE는 기밀 클라이언트에도 필수 | 승인 |
