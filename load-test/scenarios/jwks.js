// 기준선. 암호 연산과 DB 접근이 거의 없는 엔드포인트(/oauth2/jwks)로 서버·네트워크 자체의 처리 한계를 잰다.
// 다른 시나리오의 지연이 이 값보다 얼마나 큰지로 각 단계의 실제 비용을 가늠한다.
import http from 'k6/http';
import { check } from 'k6';
import { AUTH_SERVER, DEFAULT_THRESHOLDS } from '../lib/config.js';
import { writeSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    jwks: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 50),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: DEFAULT_THRESHOLDS,
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const res = http.get(`${AUTH_SERVER}/oauth2/jwks`, { tags: { name: 'jwks' } });
  check(res, { 'jwks 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return writeSummary('jwks', '기준선. JWKS 조회', data);
}
