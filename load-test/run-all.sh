#!/usr/bin/env bash
# 인증 서버(:9000, local,loadtest 프로필)와 샘플 앱 B(:8082)가 떠 있는 상태에서 실행한다.
# k6는 Docker 이미지로 실행하므로 따로 설치할 필요가 없다.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p results

for scenario in jwks login-flow token-refresh resource-api; do
  echo "==> $scenario"
  docker run --rm --network host --user "$(id -u):$(id -g)" \
    -v "$PWD:/work" -w /work \
    -e AUTH_SERVER -e APP_B -e VUS -e DURATION \
    grafana/k6:0.54.0 run "scenarios/$scenario.js" || echo "!! $scenario 임계치 미달 (결과는 results/$scenario.md)"
done

cat results/*.md > results/summary.md
echo "결과: load-test/results/summary.md"
