// Optional end-to-end UI smoke test (Playwright). Requires a running app and `npm i -D playwright`.
// Usage:  node src/test/e2e/ui-smoke.mjs [baseUrl] [screenshotDir]
// Walks the critical flows in a real browser and fails on any console error.
import { chromium } from 'playwright';

const base = process.argv[2] || 'http://localhost:8080';
const shots = process.argv[3] || null;
const errors = [];
const browser = await chromium.launch(process.env.PW_CHROMIUM ? { executablePath: process.env.PW_CHROMIUM } : {});
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('401')) errors.push(m.text()); });
page.on('pageerror', (e) => errors.push(e.message));
const snap = async (name) => { if (shots) await page.screenshot({ path: `${shots}/${name}.png`, fullPage: true }); };
const step = (s) => console.log('✓', s);

await page.goto(base + '/#/');
await page.getByRole('heading', { name: /Check it before you click/ }).waitFor();
await snap('01-landing'); step('landing page renders');

const email = `e2e-${Date.now()}@example.test`;
await page.goto(base + '/#/register');
await page.fill('#f-displayName', 'E2E Tester');
await page.fill('#f-email', email);
await page.fill('#f-password', 'short');
await page.fill('#f-confirm', 'short');
await page.click('button[type=submit]');
await page.getByText('Password must be 10-128 characters').waitFor();
step('client-side validation shows errors');
await page.fill('#f-password', 'CorrectHorse42');
await page.fill('#f-confirm', 'CorrectHorse42');
await page.click('button[type=submit]');
await page.getByRole('heading', { name: /Hello, E2E Tester/ }).waitFor();
await page.getByText('No scans yet').waitFor();
step('registration signs in and shows empty dashboard');

for (const cat of ['MESSAGE', 'URL', 'TRANSACTION', 'CALL', 'APP_PERMISSION']) {
  await page.goto(base + '/#/scan/' + cat);
  await page.getByRole('button', { name: 'Load synthetic example' }).click();
  await page.getByRole('button', { name: 'Analyze' }).click();
  await page.getByRole('heading', { name: 'Scan result' }).waitFor();
  const level = await page.locator('.result-top .badge').first().textContent();
  if (!['HIGH', 'CRITICAL'].includes(level.trim())) throw new Error(`${cat} sample expected HIGH/CRITICAL, got ${level}`);
  await snap('03-result-' + cat.toLowerCase());
  step(`${cat} scan → ${level.trim()}`);
}

await page.goto(base + '/#/scan/URL');
await page.fill('#f-url', 'https://www.wikipedia.org/');
await page.getByRole('button', { name: 'Analyze' }).click();
await page.getByRole('heading', { name: 'Scan result' }).waitFor();
if ((await page.locator('.result-top .badge').first().textContent()).trim() !== 'LOW') throw new Error('benign URL not LOW');
step('benign URL → LOW');

await page.goto(base + '/#/scan/MESSAGE');
await page.getByRole('button', { name: 'Analyze' }).click();
await page.getByText('Paste the message you want to check').waitFor();
step('empty scan blocked with message');

await page.goto(base + '/#/dashboard');
await page.getByText('Scans by category').waitFor();
await snap('02-dashboard'); step('dashboard shows charts');

await page.goto(base + '/#/history');
await page.locator('tbody tr').first().waitFor();
const rows = await page.locator('tbody tr').count();
if (rows !== 6) throw new Error('expected 6 history rows, got ' + rows);
await page.selectOption('#h-lvl', 'LOW');
await page.getByRole('button', { name: 'Apply' }).click();
await page.waitForFunction(() => document.querySelectorAll('tbody tr').length === 1);
step('history filter by level');
await snap('04-history');
await page.locator('tbody tr').first().getByRole('button', { name: /Delete/ }).click();
await page.locator('dialog').getByRole('button', { name: 'Delete' }).click();
await page.getByText('No scans match these filters').waitFor();
step('history delete with confirmation');

await page.goto(base + '/#/settings');
await page.fill('#f-retentionDays', '30');
await page.getByRole('button', { name: 'Save preferences' }).click();
await page.getByText('Preferences saved.').waitFor();
await snap('05-settings'); step('preferences saved');

await page.goto(base + '/#/about');
await page.getByText('Detection modules').waitFor();
await snap('06-about'); step('about page loads detectors');

await page.getByRole('button', { name: /Sign out/ }).click();
await page.getByRole('heading', { name: /Check it before you click/ }).waitFor();
await page.goto(base + '/#/history');
await page.getByRole('heading', { name: 'Sign in' }).waitFor();
step('logout and route guard');

await page.setViewportSize({ width: 390, height: 844 });
await page.goto(base + '/#/');
await snap('07-mobile-landing');

await browser.close();
if (errors.length) {
  console.error('Console errors:\n' + errors.join('\n'));
  process.exit(1);
}
console.log('UI smoke test passed');
