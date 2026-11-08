#!/usr/bin/env node
/**
 * API smoke for the generated app: health + OpenAPI + employee business flow.
 * Business-specific checks live here so the one-shot verify covers real behavior.
 */
const BACKEND = process.env.APP_BACKEND_URL || 'http://127.0.0.1:8080';
const FRONTEND = process.env.APP_FRONTEND_URL || 'http://127.0.0.1:5173';

async function call(path, options = {}) {
  const response = await fetch(BACKEND + path, {
    ...options,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    json = null;
  }
  return { status: response.status, text, json };
}

let failed = false;

function check(label, condition, detail) {
  console.log(`${condition ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!condition) failed = true;
}

try {
  const health = await call('/actuator/health');
  check('health', health.status === 200 && health.json?.status === 'UP', `${health.status} ${health.json?.status}`);

  const openapi = await call('/v3/api-docs');
  const paths = Object.keys(openapi.json?.paths || {});
  check('openapi', openapi.status === 200 && paths.includes('/api/employees'), `${openapi.status} paths=${paths.length}`);
  check(
    'openapi employee operations',
    ['get', 'post'].every((method) => openapi.json?.paths?.['/api/employees']?.[method]) &&
      Boolean(openapi.json?.paths?.['/api/employees/{id}']?.put),
  );

  const root = await fetch(FRONTEND + '/');
  check('frontend root', root.status === 200, String(root.status));

  const employeeNo = `E2E${Date.now()}`;
  const payload = {
    employeeNo,
    name: '联调员工',
    gender: 'FEMALE',
    department: '质量部',
    position: '测试工程师',
    phone: '13900000000',
    email: 'e2e@example.com',
    hireDate: '2025-03-01',
    status: 'ACTIVE',
  };

  const created = await call('/api/employees', { method: 'POST', body: JSON.stringify(payload) });
  check(
    'create employee',
    created.status === 200 && created.json?.code === 0 && typeof created.json?.data?.id === 'number',
    `${created.status} ${created.text.slice(0, 160)}`,
  );
  const id = created.json?.data?.id;

  const list = await call('/api/employees?page=1&size=10');
  check(
    'list employee',
    list.status === 200 &&
      list.json?.code === 0 &&
      Array.isArray(list.json?.data?.items) &&
      list.json.data.items.some((item) => item.employeeNo === employeeNo),
    `${list.status} total=${list.json?.data?.total}`,
  );

  const duplicate = await call('/api/employees', { method: 'POST', body: JSON.stringify(payload) });
  check('duplicate employeeNo -> 409', duplicate.status === 409 && duplicate.json?.code === 409, duplicate.text.slice(0, 120));

  const invalid = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ employeeNo: 'E2E-INVALID', name: '  ' }),
  });
  check(
    'validation failure -> 400 with field',
    invalid.status === 400 && invalid.json?.code === 400 && /name/.test(invalid.json?.message || ''),
    invalid.text.slice(0, 160),
  );

  const updated = await call(`/api/employees/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ ...payload, name: '联调员工已更新', status: 'INACTIVE' }),
  });
  check(
    'update employee',
    updated.status === 200 && updated.json?.data?.name === '联调员工已更新' && updated.json?.data?.status === 'INACTIVE',
    `${updated.status} ${updated.text.slice(0, 160)}`,
  );

  const missing = await call('/api/employees/99999999', { method: 'PUT', body: JSON.stringify(payload) });
  check('update missing -> 404', missing.status === 404 && missing.json?.code === 404, missing.text.slice(0, 120));

  const boundary = await call('/api/employees?page=0&size=1000');
  check(
    'boundary params clamped',
    boundary.status === 200 && boundary.json?.data?.page === 1 && boundary.json?.data?.size === 100,
    `${boundary.status} page=${boundary.json?.data?.page} size=${boundary.json?.data?.size}`,
  );
} catch (error) {
  console.error('API smoke failed:', error.message);
  failed = true;
}

if (failed) process.exitCode = 1;
else console.log('API smoke: PASS');
