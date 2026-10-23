// 시나리오 3. SDK로 보호되는 서비스 API (샘플 앱 B)
// 실제 트래픽의 대부분은 로그인이 아니라 토큰을 들고 하는 API 호출이다.
// 서비스가 인증 서버를 호출하지 않고 로컬에서 검증하므로 인증 서버 부하와 무관하게 확장되는지 본다 (ADR-005).
import http from 'k6/http';
import { check } from 'k6';
import { loginAndIssueTokens } from '../lib/oidc.js';
import { APP_B, DEFAULT_THRESHOLDS } from '../lib/config.js';
import { writeSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    api: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 50),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: DEFAULT_THRESHOLDS,
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  return { accessToken: loginAndIssueTokens().access_token };
}

export default function (data) {
  const res = http.get(`${APP_B}/api/me`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { name: 'app_b_me' },
  });
  check(res, { 'api 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return writeSummary('resource-api', '시나리오 3. SDK로 보호되는 API (샘플 앱 B)', data);
}
