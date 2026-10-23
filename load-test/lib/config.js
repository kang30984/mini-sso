// 부하 테스트 공통 설정. 환경 변수로 덮어쓸 수 있다.
export const AUTH_SERVER = __ENV.AUTH_SERVER || 'http://localhost:9000';
export const APP_B = __ENV.APP_B || 'http://127.0.0.1:8082';

// local 프로필이 기동 시 등록하는 관리자와 클라이언트 (로컬 데모 전용 값)
export const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
export const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'Local-Passw0rd!';

// 실제 사용자처럼 VU마다 다른 계정으로 로그인한다. 테스트 시작 시 관리자 API로 만든다.
// (같은 계정 하나로 동시에 로그인하면 실제와 다른 경합이 생긴다. docs/troubleshooting.md 8번)
export const USER_COUNT = Number(__ENV.USER_COUNT || 100);
export const USER_PASSWORD = 'LoadTest-Passw0rd!';
export const userFor = (vu) => `load.user${((vu - 1) % USER_COUNT) + 1}`;
export const CLIENT_ID = __ENV.CLIENT_ID || 'sample-app-a';
export const CLIENT_SECRET = __ENV.CLIENT_SECRET || 'local-sample-app-a-secret';
export const REDIRECT_URI = __ENV.REDIRECT_URI || 'http://127.0.0.1:8081/login/oauth2/code/mini-sso';

// 실패율 1% 미만을 기본 통과 기준으로 둔다
export const DEFAULT_THRESHOLDS = {
  http_req_failed: ['rate<0.01'],
  checks: ['rate>0.99'],
};
