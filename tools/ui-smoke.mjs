#!/usr/bin/env node
/**
 * UI smoke via Chrome DevTools Protocol (no npm dependency).
 * Checks the SPA mounts, then exercises the employee list/create/edit flows.
 */
import fs from 'node:fs';
import path from 'node:path';
import { Browser } from './cdp.mjs';

const URL = process.env.APP_FRONTEND_URL || 'http://127.0.0.1:5173';
const candidates = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
].filter(Boolean);
const chrome = candidates.find((candidate) => fs.existsSync(candidate));

if (!chrome) {
  if (process.env.SKIP_UI_SMOKE === '1') {
    console.log('UI smoke: SKIP (no Chrome/Edge, SKIP_UI_SMOKE=1)');
    process.exit(0);
  }
  console.error('UI smoke: no Chrome/Edge found; set CHROME_PATH or SKIP_UI_SMOKE=1');
  process.exit(1);
}

const browser = await new Browser({ chromePath: chrome, width: 1440, height: 900 }).launch();
let failed = false;

function check(label, condition, detail = '') {
  console.log(`${condition ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!condition) failed = true;
}

const dialogOpen = `(() => { const d = document.querySelector('.el-dialog'); return Boolean(d && d.offsetParent !== null); })()`;
const dialogClosed = `(() => { const d = document.querySelector('.el-dialog'); return !d || d.offsetParent === null; })()`;
const bodyText = 'document.body.innerText';

try {
  await browser.goto(URL, { waitMs: 1500 });
  await browser.waitFor('document.querySelector("#app")', 15000);
  await browser.waitFor('document.querySelector("#app").textContent.trim().length > 0', 15000);
  const info = await browser.evaluate(`({
    title: document.title,
    url: location.href,
    text: (document.querySelector('#app')?.innerText || '').trim().slice(0, 120),
  })`);
  console.log('UI:', JSON.stringify(info));
  check('SPA mounted', Boolean(info.title || info.text), info.title);

  await browser.goto(`${URL}/employees`, { waitMs: 1800 });
  await browser.waitFor('document.querySelector(".el-table")', 15000);
  await browser.waitFor('document.querySelector(".el-pagination")', 15000);
  check('员工管理页面加载', true);

  const employeeNo = `UIE2E${Date.now()}`;
  const createdName = '界面新增员工';
  const updatedName = '界面更新员工';

  await browser.click('[data-testid="employee-create"]');
  await browser.waitFor(dialogOpen, 10000);
  await browser.fill('[data-testid="employee-no"]', employeeNo);
  await browser.fill('[data-testid="employee-name"]', createdName);
  await browser.click('[data-testid="employee-submit"]');
  await browser.waitFor(dialogClosed, 15000);
  await browser.waitFor(`${bodyText}.includes(${JSON.stringify(createdName)})`, 15000);
  check('新增成功并刷新列表', true);

  await browser.click('[data-testid="employee-edit"]');
  await browser.waitFor(dialogOpen, 10000);
  await browser.evaluate('new Promise((resolve) => setTimeout(resolve, 400))');
  const prefilled = await browser.evaluate(`(() => ({
    no: document.querySelector('[data-testid="employee-no"]').value,
    name: document.querySelector('[data-testid="employee-name"]').value,
  }))()`);
  check('编辑回填', prefilled.no === employeeNo && prefilled.name === createdName, JSON.stringify(prefilled));

  await browser.fill('[data-testid="employee-name"]', updatedName);
  await browser.click('[data-testid="employee-submit"]');
  await browser.waitFor(dialogClosed, 15000);
  await browser.waitFor(`${bodyText}.includes(${JSON.stringify(updatedName)})`, 15000);
  check('编辑成功并刷新列表', true);

  await browser.click('[data-testid="employee-create"]');
  await browser.waitFor(dialogOpen, 10000);
  await browser.fill('[data-testid="employee-no"]', employeeNo);
  await browser.fill('[data-testid="employee-name"]', '重复工号员工');
  await browser.click('[data-testid="employee-submit"]');
  await browser.waitFor(`Boolean(document.querySelector('[data-testid="employee-form-error"]'))`, 15000);
  const duplicate = await browser.evaluate(`(() => ({
    open: ${dialogOpen},
    error: document.querySelector('[data-testid="employee-form-error"]').innerText,
    name: document.querySelector('[data-testid="employee-name"]').value,
  }))()`);
  check(
    '工号重复提示且弹窗保留',
    duplicate.open && duplicate.error.includes('工号已存在') && duplicate.name === '重复工号员工',
    JSON.stringify(duplicate),
  );
  await browser.click('[data-testid="employee-cancel"]');
  await browser.waitFor(dialogClosed, 10000);

  await browser.click('[data-testid="employee-create"]');
  await browser.waitFor(dialogOpen, 10000);
  await browser.click('[data-testid="employee-submit"]');
  await browser.waitFor(`document.querySelectorAll('.el-form-item__error').length >= 2`, 10000);
  const fieldErrors = await browser.evaluate(`(() => ({
    text: document.querySelector('.el-dialog').innerText,
    count: document.querySelectorAll('.el-form-item__error').length,
  }))()`);
  check(
    '必填校验拦截并提示字段',
    fieldErrors.text.includes('请输入工号') && fieldErrors.text.includes('请输入姓名'),
    JSON.stringify({ count: fieldErrors.count }),
  );
  await browser.click('[data-testid="employee-cancel"]');

  if (process.env.UI_SMOKE_SCREENSHOT) {
    const file = path.resolve('verify', 'ui-smoke.png');
    fs.mkdirSync(path.dirname(file), { recursive: true });
    await browser.screenshot(file);
    console.log('screenshot:', file);
  }

  const errors = browser.consoleLogs.filter((entry) => entry.type === 'error' && !/favicon/i.test(entry.text));
  if (browser.exceptions.length || errors.length) {
    console.error('browser errors:', browser.exceptions.join(' | '), errors.map((entry) => entry.text).join(' | '));
    failed = true;
  }
} catch (error) {
  console.error('UI smoke failed:', error.message);
  failed = true;
} finally {
  await browser.close();
}
if (failed) process.exitCode = 1;
else console.log('UI smoke: PASS');
