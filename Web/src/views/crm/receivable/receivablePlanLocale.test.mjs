import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./plan/index.vue', import.meta.url), 'utf8')
const zh = readFileSync(new URL('../../../locales/zh-CN/crm.ts', import.meta.url), 'utf8')
const en = readFileSync(new URL('../../../locales/en/crm.ts', import.meta.url), 'utf8')
const ar = readFileSync(new URL('../../../locales/ar/crm.ts', import.meta.url), 'utf8')

test('receivable plan contract number placeholder resolves in every locale', () => {
  assert.match(view, /t\('receivablePlan\.contractNoPlaceholder'\)/)
  assert.match(zh, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: '请输入合同编号'/)
  assert.match(en, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: 'Enter contract number'/)
  assert.match(ar, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: 'أدخل رقم العقد'/)
})
