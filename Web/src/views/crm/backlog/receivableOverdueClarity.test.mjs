import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const backlog = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const list = readFileSync(new URL('./components/ReceivablePlanRemindList.vue', import.meta.url), 'utf8')
const zh = readFileSync(new URL('../../../locales/zh-CN/crm.ts', import.meta.url), 'utf8')
const en = readFileSync(new URL('../../../locales/en/crm.ts', import.meta.url), 'utf8')
const ar = readFileSync(new URL('../../../locales/ar/crm.ts', import.meta.url), 'utf8')

test('receivable plans use one backlog entry and filter status inside the workspace', () => {
  assert.match(backlog, /leftMenu === 'receivablePlanRemind'/)
  assert.match(backlog, /getReceivablePlanRemindCount/)
  assert.doesNotMatch(backlog, /leftMenu === 'receivablePlanPending'/)
  assert.doesNotMatch(backlog, /leftMenu === 'receivablePlanOverdue'/)
  assert.match(list, /v-model="queryParams\.remindType"/)
  assert.match(list, /RECEIVABLE_REMIND_TYPE/)
})

test('the consolidated entry has explicit labels in all supported locales', () => {
  assert.match(zh, /receivablePlanRemind: '回款计划与逾期'/)
  assert.match(en, /receivablePlanRemind: 'Receivable Plans & Overdue'/)
  assert.match(ar, /receivablePlanRemind: 'خطط التحصيل والمتأخرات'/)
})

test('overdue workspace explains urgency and exposes overdue days', () => {
  assert.match(list, /receivablePlanOverdueGuidance/)
  assert.match(list, /getOverdueDays/)
  assert.match(list, /backlog\.overdueDaysValue/)
  assert.match(list, /backlog\.registerReceivable/)
})
