// Frontend unit tests (no dependencies): node --test src/test/frontend
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateEmail, validatePassword, validateRegistration, validateScan, parsePermissionList, looksLikeCardNumber, luhn }
  from '../../main/resources/static/js/validation.js';

test('email validation', () => {
  assert.equal(validateEmail('student@example.test'), null);
  assert.ok(validateEmail(''));
  assert.ok(validateEmail('no-at-sign'));
});

test('password rules mirror the server', () => {
  assert.equal(validatePassword('CorrectHorse42'), null);
  assert.ok(validatePassword('short1'));
  assert.ok(validatePassword('onlyletterslong'));
  assert.ok(validatePassword('1234567890123'));
});

test('registration reports every invalid field and mismatched confirmation', () => {
  const e = validateRegistration({ displayName: 'A', email: 'x', password: 'abc', confirm: 'abd' });
  assert.deepEqual(Object.keys(e).sort(), ['confirm', 'displayName', 'email', 'password']);
  assert.deepEqual(validateRegistration({ displayName: 'Ana', email: 'a@b.test', password: 'GoodPass123', confirm: 'GoodPass123' }), {});
});

test('scan validation per category', () => {
  assert.ok(validateScan('MESSAGE', { text: '   ' }).text);
  assert.ok(validateScan('MESSAGE', { text: 'x'.repeat(5001) }).text);
  assert.deepEqual(validateScan('MESSAGE', { text: 'hello' }), {});
  assert.ok(validateScan('URL', { url: 'http://a b.com' }).url);
  assert.deepEqual(validateScan('URL', { url: 'https://example.com' }), {});
  assert.ok(validateScan('TRANSACTION', { amount: '0' }).amount);
  assert.ok(validateScan('TRANSACTION', { amount: '10', currency: 'rupees' }).currency);
  assert.ok(validateScan('TRANSACTION', { amount: '10', attemptsLastHour: '2.5' }).attemptsLastHour);
  assert.deepEqual(validateScan('TRANSACTION', { amount: '10', currency: 'INR', attemptsLastHour: '' }), {});
  assert.ok(validateScan('CALL', { description: '' }).description);
  assert.ok(validateScan('CALL', { description: 'ok', callerNumber: 'abc' }).callerNumber);
  assert.ok(validateScan('APP_PERMISSION', { appName: 'x', permissions: [] }).permissions);
  assert.ok(validateScan('APP_PERMISSION', { appName: 'x', permissions: ['READ SMS<'] }).permissions);
  assert.deepEqual(validateScan('APP_PERMISSION', { appName: 'x', permissions: ['android.permission.READ_SMS'] }), {});
  assert.ok(validateScan('NOPE', {}).category);
});

test('permission list parsing de-duplicates and splits on separators', () => {
  assert.deepEqual(parsePermissionList('READ_SMS, CAMERA\nREAD_SMS;INTERNET'), ['READ_SMS', 'CAMERA', 'INTERNET']);
  assert.deepEqual(parsePermissionList(''), []);
});

test('card number detection uses Luhn', () => {
  assert.equal(luhn('4111111111111111'), true);
  assert.equal(luhn('4111111111111112'), false);
  assert.equal(looksLikeCardNumber('card 4111 1111 1111 1111'), true);
  assert.equal(looksLikeCardNumber('order 1234567890123'), false);
});
