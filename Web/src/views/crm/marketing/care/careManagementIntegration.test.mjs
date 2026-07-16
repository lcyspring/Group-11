import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const page = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../../api/crm/marketing/index.ts', import.meta.url), 'utf8')
const locales = await Promise.all(['zh-CN', 'en', 'ar'].map((locale) =>
  readFile(new URL(`../../../../locales/${locale}/crm.ts`, import.meta.url), 'utf8')))

test('care page exposes plan maintenance, records, and birthday query tabs', () => {
  assert.match(page, /name="plans"/)
  assert.match(page, /name="records"/)
  assert.match(page, /name="birthdays"/)
  assert.match(page, /getCareRecordPage/)
  assert.match(page, /getCustomerBirthdayPage/)
})

test('plan maintenance includes detail edit, status, guarded delete, and menu actions', () => {
  assert.match(api, /care\/plan\/get/)
  assert.match(api, /care\/plan\/status/)
  assert.match(api, /care\/plan\/delete/)
  assert.match(page, /<TableActions[\s\S]*?mode="menu"/)
  assert.match(page, /crm:customer-care:delete/)
  assert.match(page, /:disabled="row\.enabled"/)
  assert.match(page, /!checkPermi\(\['crm:customer-care:update'\]\)/)
})

test('all supported locales describe the complete care workflow', () => {
  for (const locale of locales) {
    assert.match(locale, /carePlans:/)
    assert.match(locale, /careRecords:/)
    assert.match(locale, /upcomingBirthdays:/)
    assert.match(locale, /postDealFollowUp:/)
    assert.match(locale, /careConsentHint:/)
  }
})
