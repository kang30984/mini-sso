// 시나리오 1. 전체 로그인 흐름 (인가 요청 -> 로그인 -> 코드 -> 토큰 교환)
// 반복마다 새 세션으로 로그인한다. 사용자가 몰리는 출근 시간대의 로그인 폭주를 가정한다.
import { createLoadTestUsers, loginAndIssueTokens } from '../lib/oidc.js';
import { DEFAULT_THRESHOLDS, USER_PASSWORD, userFor } from '../lib/config.js';
import { writeSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    login: {
      executor: 'ramping-vus',
      stages: [
        { duration: '15s', target: Number(__ENV.VUS || 20) },
        { duration: __ENV.DURATION || '60s', target: Number(__ENV.VUS || 20) },
        { duration: '5s', target: 0 },
      ],
    },
  },
  thresholds: DEFAULT_THRESHOLDS,
  setupTimeout: '180s',
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  createLoadTestUsers();
}

export default function () {
  loginAndIssueTokens(userFor(__VU), USER_PASSWORD);
}

export function handleSummary(data) {
  return writeSummary('login-flow', '시나리오 1. 전체 로그인 흐름', data);
}
