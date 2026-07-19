import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'

const constants = readFileSync(new URL('./constants.ts', import.meta.url), 'utf8')
const form = readFileSync(new URL('./RefundForm.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/refund/index.ts', import.meta.url), 'utf8')

test('refund commands use the epoch-millisecond LocalDateTime contract', () => {
  assert.match(constants, /const timestamp = Number\(value\)/)
  assert.match(form, /value-format="x"/)
  assert.match(form, /refundTime: Date\.now\(\)/)
  assert.match(form, /refundTime: toRefundEpochMillis\(formData\.value\.refundTime\)/)
  assert.match(api, /refundTime: number/)
})

test('refund create and update submit an explicit command object', () => {
  assert.match(form, /const command: RefundApi\.ReceivableRefundSaveReqVO/)
  assert.match(form, /RefundApi\.createRefund\(command\)/)
  assert.match(form, /RefundApi\.updateRefund\(command\)/)
  assert.doesNotMatch(form, /RefundApi\.createRefund\(formData\.value\)/)
})
