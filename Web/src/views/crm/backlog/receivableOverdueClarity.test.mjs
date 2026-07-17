import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const backlog = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const list = readFileSync(new URL('./components/ReceivablePlanRemindList.vue', import.meta.url), 'utf8')

test('pending and overdue receivable plans have distinct backlog entries', () => {
  assert.match(backlog, /receivablePlanPending/)
  assert.match(backlog, /receivablePlanOverdue/)
  assert.match(backlog, /initial-remind-type="1"/)
  assert.match(backlog, /initial-remind-type="2"/)
})

test('overdue workspace explains urgency and exposes overdue days', () => {
  assert.match(list, /receivablePlanOverdueGuidance/)
  assert.match(list, /getOverdueDays/)
  assert.match(list, /backlog\.overdueDaysValue/)
  assert.match(list, /backlog\.registerReceivable/)
})
