# 에러 코드

모든 오류 응답은 RFC 9457 `ProblemDetail` 형식이며 `code` 필드로 아래 코드를 돌려준다.
정의 위치: `auth-domain`의 `ErrorCode`, HTTP 상태 매핑: `auth-adapter-in-web`의 `ErrorStatus.kt`.

```json
{
  "type": "https://github.com/kang30984/mini-sso/blob/main/docs/error-codes.md",
  "title": "DUPLICATE_LOGIN_ID",
  "status": 409,
  "detail": "이미 사용 중인 로그인 ID입니다.",
  "instance": "/api/admin/v1/accounts",
  "code": "ACCOUNT-201"
}
```

| 코드 | 이름 | HTTP | 설명 |
|---|---|---|---|
| COMMON-001 | INVALID_INPUT | 400 | 요청 형식 오류. 필드 검증 실패 시 `errors` 배열 포함 |
| COMMON-500 | (없음) | 500 | 예상하지 못한 오류. 내부 정보는 응답에 포함하지 않음 |
| ACCOUNT-001 | INVALID_LOGIN_ID | 400 | 로그인 ID 형식 위반 |
| ACCOUNT-002 | INVALID_EMAIL | 400 | 이메일 형식 위반 |
| ACCOUNT-003 | PASSWORD_POLICY_VIOLATION | 400 | 비밀번호 정책 위반 |
| ACCOUNT-004 | CURRENT_PASSWORD_MISMATCH | 400 | 비밀번호 변경 시 현재 비밀번호 불일치 |
| ACCOUNT-005 | PASSWORD_REUSED | 400 | 현재와 같은 비밀번호로 변경 시도 |
| ACCOUNT-006 | INVALID_PROFILE | 400 | 이름·부서 길이 위반 |
| ACCOUNT-101 | ACCOUNT_NOT_FOUND | 404 | 계정 없음 |
| ACCOUNT-102 | ACCOUNT_NOT_ACTIVE | 409 | 휴면·탈퇴 계정에 대한 변경 시도 |
| ACCOUNT-201 | DUPLICATE_LOGIN_ID | 409 | 로그인 ID 중복 |
| ACCOUNT-202 | DUPLICATE_EMAIL | 409 | 이메일 중복 |
| ACCOUNT-203 | DUPLICATE_ACCOUNT | 409 | 동시 가입으로 유니크 제약 위반 |
| HISTORY-001 | INVALID_SEARCH_PERIOD | 400 | 이력 조회 기간 오류 (최대 31일) |
| CLIENT-001 | INVALID_CLIENT_ID | 400 | 클라이언트 ID 형식 위반 |
| CLIENT-002 | INVALID_CLIENT_SECRET | 400 | 클라이언트 시크릿 16자 미만 |
| CLIENT-003 | INVALID_REDIRECT_URI | 400 | 리다이렉트 URI 정책 위반 (http는 로컬 주소만, 와일드카드·fragment 금지) |
| CLIENT-004 | INVALID_SCOPE | 400 | openid 누락 또는 지원하지 않는 scope |
| CLIENT-005 | INVALID_CLIENT_NAME | 400 | 클라이언트 이름 길이 위반 |
| CLIENT-201 | DUPLICATE_CLIENT_ID | 409 | 클라이언트 ID 중복 |

### SDK (리소스 서버)

SDK를 적용한 서비스는 인증·인가 실패를 같은 형식으로 응답한다.

| 코드 | HTTP | 설명 |
|---|---|---|
| MINISSO-401 | 401 | 토큰 없음, 만료, 서명·클레임 검증 실패. `WWW-Authenticate: Bearer` 헤더 포함 |
| MINISSO-403 | 403 | `@RequiresScope`의 scope 또는 권한 부족 |

로그인 실패는 에러 코드가 아니라 인증 결과로 다룬다. 사용자에게는 계정 존재 여부가 드러나지 않도록
`UNKNOWN_ACCOUNT`, `WITHDRAWN`, `BAD_CREDENTIALS`를 같은 메시지로 보여준다 (로그인 화면에 적용됨. 잠금과 휴면은 사용자 조치가 필요하므로 구분해서 안내한다).
