import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/receivable/index.ts', import.meta.url), 'utf8')

test('receivable page exposes approved-only write-off flow with bounded remaining amount', () => {
  assert.match(view, /scope\.row\.auditStatus === 20/)
  assert.match(view, /crm:receivable:write-off/)
  assert.match(view, /writeOffRemaining/)
  assert.match(view, /createReceivableWriteOff/)
  assert.match(api, /write-off\/create/)
  assert.match(api, /write-off\/reverse/)
})
