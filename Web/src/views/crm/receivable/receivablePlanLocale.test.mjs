import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./plan/index.vue', import.meta.url), 'utf8')
const customerPlanList = readFileSync(new URL('./plan/components/ReceivablePlanList.vue', import.meta.url), 'utf8')
const receivableIndex = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const receivableList = readFileSync(new URL('./components/ReceivableList.vue', import.meta.url), 'utf8')
const receivableForm = readFileSync(new URL('./ReceivableForm.vue', import.meta.url), 'utf8')
const zh = readFileSync(new URL('../../../locales/zh-CN/crm.ts', import.meta.url), 'utf8')
const en = readFileSync(new URL('../../../locales/en/crm.ts', import.meta.url), 'utf8')
const ar = readFileSync(new URL('../../../locales/ar/crm.ts', import.meta.url), 'utf8')

test('receivable plan contract number placeholder resolves in every locale', () => {
  assert.match(view, /t\('receivablePlan\.contractNoPlaceholder'\)/)
  assert.match(zh, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: '请输入合同编号'/)
  assert.match(en, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: 'Enter contract number'/)
  assert.match(ar, /receivablePlan:\s*\{[\s\S]*contractNoPlaceholder: 'أدخل رقم العقد'/)
})

test('customer detail receivable plan action has a complete translation contract', () => {
  assert.match(customerPlanList, /t\('receivablePlan\.createReceivablePlan'\)/)
  assert.match(zh, /createReceivablePlan: '新建回款计划'/)
  assert.match(en, /createReceivablePlan: 'Create Receivable Plan'/)
  assert.match(ar, /createReceivablePlan: 'إنشاء خطة تحصيل'/)
})

test('receivable amount and period labels do not embed Chinese presentation text', () => {
  for (const source of [view, customerPlanList, receivableIndex, receivableList]) {
    assert.doesNotMatch(source, /\+ '（元）'/)
  }
  assert.doesNotMatch(receivableForm, /'第 ' \+ data\.period \+ ' 期'/)
  assert.match(receivableForm, /t\('receivable\.periodOption'/)
  assert.match(customerPlanList, /t\('receivablePlan\.amountInCny'/)
})

test('all supported locales define receivable currency and period presentation', () => {
  for (const locale of [zh, en, ar]) {
    const receivable = locale.slice(locale.indexOf('  receivable: {'), locale.indexOf('  receivablePlan: {'))
    const plan = locale.slice(locale.indexOf('  receivablePlan: {'), locale.indexOf('  followUp: {'))
    assert.match(receivable, /periodOption:/)
    assert.match(receivable, /amountInCny:/)
    assert.match(plan, /createReceivablePlan:/)
    assert.match(plan, /amountInCny:/)
  }
})
