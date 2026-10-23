# 부하 테스트 (k6)

## 시나리오

| 시나리오 | 내용 | 주로 측정하는 비용 |
|---|---|---|
| `jwks` (기준선) | 공개키 조회 | 서버·네트워크 자체의 처리 한계 |
| `login-flow` | 인가 요청 → 로그인 → 인가 코드 → 토큰 교환 (매번 새 세션) | Argon2 비밀번호 비교, 세션 생성, 클라이언트 인증, JWT 서명 |
| `token-refresh` | 로그인 1회 후 리프레시 토큰 회전 반복 | 클라이언트 인증, Redis 회전 기록, JWT 서명 |
| `resource-api` | 샘플 앱 B API 호출 (SDK 로컬 검증) | 서비스 쪽 JWT 검증 |

`login-flow`는 단계별 지연(`step_authorize`, `step_login`, `step_issue_code`, `step_token_exchange`)을 따로 기록해 어느 단계가 병목인지 볼 수 있다.

## 실행

### GitHub Actions (권장)

Actions 탭 → **Load Test** → **Run workflow**. 결과는 실행 화면의 Summary에 표로 나오고, 상세 JSON과 서버 로그는 Artifacts(`load-test-results`)로 받을 수 있다.

매번 같은 사양의 러너에서 실행되므로 변경 전후 비교에 쓴다. 단, MySQL·Redis·인증 서버·샘플 앱·k6가 모두 한 대에서 돌아 자원을 나눠 쓰므로 절대 수치는 운영 환경보다 낮다.

### 로컬

```bash
docker compose up -d
./gradlew :auth-server:auth-bootstrap:bootRun --args='--spring.profiles.active=local,loadtest'
./gradlew :samples:sample-app-b:bootRun
./load-test/run-all.sh          # Docker로 k6 실행, 결과: load-test/results/summary.md
```

`loadtest` 프로필은 IP당 로그인 시도 제한을 끈다. 부하 도구가 한 IP에서 요청하기 때문이다.

## 측정 시 주의

- 실패율(`http_req_failed`)은 `setup()`의 요청도 포함한다. 테스트 계정 생성 시 이미 있는 계정의 409를 실패로 세지 않도록 `expectedStatuses(201, 409)`를 지정했다. (지정 전에는 토큰 갱신 시나리오에서 처리량과 무관하게 항상 약 100건의 "실패"가 집계되었다)
- 같은 계정 하나로 동시에 로그인하면 실제와 다른 경합이 생긴다. 가상 사용자마다 다른 계정을 쓴다.
- 비교는 같은 커밋에서 설정 하나만 바꿔 연속으로 실행한다. 기준선(JWKS) 처리량이 비슷한지로 두 실행의 환경이 동등했는지 확인한다.

## 결과 기록

측정 결과와 분석은 루트 README의 "성능" 절에 기록한다.
