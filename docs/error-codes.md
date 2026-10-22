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

로그인 실패는 에러 코드가 아니라 인증 결과로 다룬다. 사용자에게는 계정 존재 여부가 드러나지 않도록
`UNKNOWN_ACCOUNT`, `WITHDRAWN`, `BAD_CREDENTIALS`를 같은 메시지로 보여준다 (3주차 로그인 화면에서 적용).
