#!/usr/bin/env node
/*
 * villa-reservation PreToolUse guard
 * 하네스 안전장치 — 다음을 차단한다(브리프 AI 규칙 #3, #4와 정합):
 *   1) Anthropic API 키(sk-ant-...) 평문이 파일에 박히는 것
 *   2) application.yml 에 anthropic api-key 가 ${ENV} 가 아닌 실제값으로 들어가는 것
 *   3) 가족 실데이터(H2 DB, villa-reservation.mv.db)를 Write 로 덮어쓰는 것
 *
 * 입력: stdin 으로 PreToolUse JSON (tool_name, tool_input)
 * 출력: 통과=exit 0 / 차단=exit 2 + stderr 메시지(에이전트에게 전달)
 */
'use strict';

function readStdin() {
  return new Promise(function (resolve) {
    var data = '';
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', function (c) { data += c; });
    process.stdin.on('end', function () { resolve(data); });
    // stdin 이 없으면 즉시 통과(안전)
    setTimeout(function () { if (!data) resolve(''); }, 1500);
  });
}

function collectText(input) {
  if (!input) return '';
  var parts = [];
  if (typeof input.content === 'string') parts.push(input.content);
  if (typeof input.new_string === 'string') parts.push(input.new_string);
  if (Array.isArray(input.edits)) {
    input.edits.forEach(function (e) {
      if (e && typeof e.new_string === 'string') parts.push(e.new_string);
    });
  }
  return parts.join('\n');
}

function block(msg) {
  process.stderr.write('[villa-guard] 차단: ' + msg + '\n');
  process.exit(2);
}

readStdin().then(function (raw) {
  var payload;
  try { payload = JSON.parse(raw || '{}'); } catch (e) { process.exit(0); }

  var tool = payload.tool_name || '';
  if (['Write', 'Edit', 'MultiEdit'].indexOf(tool) === -1) process.exit(0);

  var input = payload.tool_input || {};
  var filePath = (input.file_path || '').replace(/\\/g, '/');
  var text = collectText(input);

  // 3) H2 실데이터 덮어쓰기 차단 (Write 로 .mv.db 생성/교체)
  if (tool === 'Write' && /villa-reservation\.mv\.db$/i.test(filePath)) {
    block('가족 실데이터(H2 DB)를 Write 로 덮어쓸 수 없습니다 — 백업/별도 경로를 쓰세요.');
  }

  // 1) Anthropic API 키 평문 차단
  if (/sk-ant-[A-Za-z0-9_\-]{20,}/.test(text)) {
    block('Anthropic API 키 평문이 감지되었습니다. 코드/설정에 키를 박지 말고 ${ANTHROPIC_API_KEY} 환경변수를 쓰세요.');
  }

  // 2) application.yml 에 api-key 실제값 차단 ( ${...} 만 허용 )
  if (/application(-\w+)?\.ya?ml$/i.test(filePath)) {
    var m = text.match(/api-key\s*:\s*([^\s#]+)/i);
    if (m && m[1] && m[1].indexOf('${') !== 0) {
      block('application.yml 의 api-key 는 ${ANTHROPIC_API_KEY:} 형태여야 합니다 (실제 키 금지).');
    }
  }

  process.exit(0);
}).catch(function () { process.exit(0); });
