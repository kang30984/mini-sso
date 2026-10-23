// 시나리오 2. 토큰 갱신 (리프레시 토큰 회전)
// 각 VU가 한 번 로그인한 뒤 갱신을 반복한다. 매 갱신마다 새 리프레시 토큰을 받아 다음 갱신에 쓴다.
// 클라이언트 인증, Redis 회전 기록, JWT 서명 비용을 측정한다.
import { loginAndIssueTokens, refresh } from '../lib/oidc.js';
import { DEFAULT_THRESHOLDS } from '../lib/config.js';
import { writeSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    refresh: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: DEFAULT_THRESHOLDS,
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

let refreshToken = null;

export default function () {
  if (!refreshToken) {
    refreshToken = loginAndIssueTokens().refresh_token;
  }
  const refreshed = refresh(refreshToken);
  refreshToken = refreshed ? refreshed.refresh_token : null;
}

export function handleSummary(data) {
  return writeSummary('token-refresh', '시나리오 2. 토큰 갱신 (회전)', data);
}
