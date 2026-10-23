// 부하 테스트 공통 설정. 환경 변수로 덮어쓸 수 있다.
export const AUTH_SERVER = __ENV.AUTH_SERVER || 'http://localhost:9000';
export const APP_B = __ENV.APP_B || 'http://127.0.0.1:8082';

// local 프로필이 기동 시 등록하는 계정과 클라이언트 (로컬 데모 전용 값)
export const USERNAME = __ENV.USERNAME || 'admin';
export const PASSWORD = __ENV.PASSWORD || 'Local-Passw0rd!';
export const CLIENT_ID = __ENV.CLIENT_ID || 'sample-app-a';
export const CLIENT_SECRET = __ENV.CLIENT_SECRET || 'local-sample-app-a-secret';
export const REDIRECT_URI = __ENV.REDIRECT_URI || 'http://127.0.0.1:8081/login/oauth2/code/mini-sso';

// 실패율 1% 미만을 기본 통과 기준으로 둔다
export const DEFAULT_THRESHOLDS = {
  http_req_failed: ['rate<0.01'],
  checks: ['rate>0.99'],
};
