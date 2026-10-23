// k6 결과를 사람이 읽을 수 있는 Markdown 표로 만든다 (GitHub Actions Job Summary에 표시)
function fmt(value) {
  return value === undefined ? '-' : value.toFixed(1);
}

export function markdownSummary(title, data) {
  const lines = [`### ${title}`, ''];
  const reqs = data.metrics.http_reqs;
  const iterations = data.metrics.iterations;
  const failed = data.metrics.http_req_failed;
  lines.push(`- 처리량: **${fmt(iterations ? iterations.values.rate : 0)} iterations/s**, ${fmt(reqs ? reqs.values.rate : 0)} req/s`);
  lines.push(`- 실패율: ${failed ? (failed.values.rate * 100).toFixed(2) : '-'}%`);
  lines.push('');
  lines.push('| 지표 | 평균(ms) | p50 | p95 | p99 | 최대 |');
  lines.push('|---|---|---|---|---|---|');
  Object.keys(data.metrics)
    .filter((name) => name.startsWith('step_') || name === 'http_req_duration' || name === 'iteration_duration')
    .sort()
    .forEach((name) => {
      const v = data.metrics[name].values;
      lines.push(`| ${name} | ${fmt(v.avg)} | ${fmt(v.med)} | ${fmt(v['p(95)'])} | ${fmt(v['p(99)'])} | ${fmt(v.max)} |`);
    });
  lines.push('');
  return lines.join('\n');
}

export function writeSummary(name, title, data) {
  return {
    stdout: markdownSummary(title, data),
    [`results/${name}.md`]: markdownSummary(title, data),
    [`results/${name}.json`]: JSON.stringify(data, null, 2),
  };
}
