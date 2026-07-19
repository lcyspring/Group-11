import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const dialog = readFileSync(new URL('./ReceivableWriteOffDialog.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/receivable/index.ts', import.meta.url), 'utf8')

test('receivable page exposes approved-only write-off flow with bounded remaining amount', () => {
  assert.match(view, /scope\.row\.auditStatus === 20/)
  assert.match(view, /crm:receivable:write-off/)
  assert.match(view, /remainingWriteOffAmount/)
  assert.match(view, /Number\(scope\.row\.remainingWriteOffAmount/)
  assert.match(view, /ReceivableWriteOffDialog/)
  assert.match(dialog, /value-format="x"/)
  assert.doesNotMatch(dialog, /value-format="YYYY-MM-DD HH:mm:ss"/)
  assert.match(dialog, /writeOffTime: Date\.now\(\)/)
  assert.match(dialog, /remainingAmount/)
  assert.match(dialog, /refreshRecords/)
  assert.match(dialog, /reverseReceivableWriteOff/)
  assert.match(dialog, /createReceivableWriteOff/)
  assert.match(api, /write-off\/create/)
  assert.match(api, /write-off\/reverse/)
})
