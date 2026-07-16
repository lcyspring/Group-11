import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const receivableForm = await readFile(new URL('./ReceivableForm.vue', import.meta.url), 'utf8')
const planForm = await readFile(new URL('./plan/ReceivablePlanForm.vue', import.meta.url), 'utf8')
const locales = await Promise.all([
  readFile(new URL('../../../locales/zh-CN/crm.ts', import.meta.url), 'utf8'),
  readFile(new URL('../../../locales/en/crm.ts', import.meta.url), 'utf8'),
  readFile(new URL('../../../locales/ar/crm.ts', import.meta.url), 'utf8')
])

test('receivable forms use field-specific select placeholders', () => {
  for (const source of [receivableForm, planForm]) {
    assert.match(source, /receivable\.ownerUserId/)
    assert.match(source, /receivable\.customerPlaceholder/)
    assert.match(source, /receivable\.contractPlaceholder/)
    assert.doesNotMatch(source, /customer\.ownerUserPlaceholder/)
    assert.doesNotMatch(source, /contract\.namePlaceholder/)
  }
})

test('all supported locales define receivable owner and select placeholders', () => {
  for (const locale of locales) {
    const section = locale.slice(locale.indexOf('  receivable: {'), locale.indexOf('  receivablePlan: {'))
    assert.match(section, /ownerUserId:/)
    assert.match(section, /customerPlaceholder:/)
    assert.match(section, /contractPlaceholder:/)
  }
})
