// 브라우저가 하는 OIDC 인가 코드 + PKCE 흐름을 k6로 재현한다.
// 리다이렉트는 직접 따라간다 (클라이언트 redirect_uri는 실제로 호출하지 않는다).
import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { check, fail } from 'k6';
import { Trend } from 'k6/metrics';
import {
  ADMIN_PASSWORD, ADMIN_USERNAME, AUTH_SERVER, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_COUNT, USER_PASSWORD,
} from './config.js';

export const steps = {
  authorize: new Trend('step_authorize', true),
  loginPage: new Trend('step_login_page', true),
  login: new Trend('step_login', true),
  code: new Trend('step_issue_code', true),
  token: new Trend('step_token_exchange', true),
  refresh: new Trend('step_token_refresh', true),
};

const NO_REDIRECT = { redirects: 0 };
const VERIFIER_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';

function randomVerifier() {
  let value = '';
  for (let i = 0; i < 64; i++) {
    value += VERIFIER_CHARS.charAt(Math.floor(Math.random() * VERIFIER_CHARS.length));
  }
  return value;
}

function basicAuth() {
  return `Basic ${encoding.b64encode(`${CLIENT_ID}:${CLIENT_SECRET}`)}`;
}

function authorizeUrl(challenge, state) {
  const params = {
    response_type: 'code',
    client_id: CLIENT_ID,
    scope: 'openid profile email',
    redirect_uri: REDIRECT_URI,
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  };
  const query = Object.keys(params).map((k) => `${k}=${encodeURIComponent(params[k])}`).join('&');
  return `${AUTH_SERVER}/oauth2/authorize?${query}`;
}

function absolute(location) {
  return location.startsWith('http') ? location : `${AUTH_SERVER}${location}`;
}

function queryParam(url, name) {
  const match = url.match(new RegExp(`[?&]${name}=([^&]+)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/** 로그인부터 토큰 발급까지. 반환: 토큰 응답 JSON */
export function loginAndIssueTokens(username = ADMIN_USERNAME, password = ADMIN_PASSWORD) {
  http.cookieJar().clear(AUTH_SERVER);
  const verifier = randomVerifier();
  const challenge = crypto.sha256(verifier, 'base64rawurl');
  const state = `s${Math.floor(Math.random() * 1e9)}`;

  // 1. 인가 요청 -> 로그인 페이지로
  let res = http.get(authorizeUrl(challenge, state), Object.assign({ tags: { name: 'authorize' } }, NO_REDIRECT));
  steps.authorize.add(res.timings.duration);
  if (!check(res, { 'authorize -> login 302': (r) => r.status === 302 })) fail(`authorize ${res.status}`);

  // 2. 로그인 페이지 (CSRF 토큰)
  res = http.get(absolute(res.headers.Location), { tags: { name: 'login_page' } });
  steps.loginPage.add(res.timings.duration);
  const csrf = res.html().find('input[name=_csrf]').attr('value');
  if (!check(csrf, { 'csrf token': (v) => !!v })) fail('no csrf token');

  // 3. 로그인 (Argon2 비밀번호 비교, 세션 생성)
  res = http.post(`${AUTH_SERVER}/login`, { username, password, _csrf: csrf },
    Object.assign({ tags: { name: 'login' } }, NO_REDIRECT));
  steps.login.add(res.timings.duration);
  const afterLogin = res.headers.Location || '';
  if (!check(res, { 'login success': (r) => r.status === 302 && !afterLogin.includes('error') })) {
    fail(`login ${res.status} ${afterLogin}`);
  }

  // 4. 같은 세션으로 다시 인가 요청 -> 인가 코드
  res = http.get(absolute(afterLogin), Object.assign({ tags: { name: 'issue_code' } }, NO_REDIRECT));
  steps.code.add(res.timings.duration);
  const callback = res.headers.Location || '';
  const code = queryParam(callback, 'code');
  if (!check(res, { 'code issued': () => !!code && queryParam(callback, 'state') === state })) {
    fail(`code ${res.status} ${callback}`);
  }

  // 5. 토큰 교환 (클라이언트 인증 + PKCE 검증 + JWT 서명 + Redis 저장)
  res = http.post(`${AUTH_SERVER}/oauth2/token`, {
    grant_type: 'authorization_code',
    code,
    redirect_uri: REDIRECT_URI,
    code_verifier: verifier,
  }, { headers: { Authorization: basicAuth() }, tags: { name: 'token_exchange' } });
  steps.token.add(res.timings.duration);
  if (!check(res, { 'token issued': (r) => r.status === 200 })) fail(`token ${res.status} ${res.body}`);
  return res.json();
}

/** 리프레시 토큰으로 갱신 (회전). 반환: 새 토큰 응답 JSON 또는 null */
export function refresh(refreshToken) {
  const res = http.post(`${AUTH_SERVER}/oauth2/token`, {
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
  }, { headers: { Authorization: basicAuth() }, tags: { name: 'token_refresh' } });
  steps.refresh.add(res.timings.duration);
  return check(res, { 'refreshed': (r) => r.status === 200 }) ? res.json() : null;
}

/** 관리자로 로그인해 부하 테스트용 계정을 만든다. 이미 있으면(409) 건너뛴다. setup()에서 호출한다. */
export function createLoadTestUsers() {
  const adminToken = loginAndIssueTokens().access_token;
  for (let i = 1; i <= USER_COUNT; i++) {
    const loginId = `load.user${i}`;
    const res = http.post(`${AUTH_SERVER}/api/admin/v1/accounts`, JSON.stringify({
      loginId,
      email: `${loginId}@example.com`,
      name: `부하테스트${i}`,
      department: '부하테스트',
      initialPassword: USER_PASSWORD,
    }), {
      headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
      tags: { name: 'setup_create_user' },
      // 앞 시나리오가 이미 만든 계정이면 409. 정상 응답이므로 실패율에 넣지 않는다
      responseCallback: http.expectedStatuses(201, 409),
    });
    if (res.status !== 201 && res.status !== 409) fail(`create ${loginId}: ${res.status} ${res.body}`);
  }
}
