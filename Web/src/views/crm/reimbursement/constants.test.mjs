import assert from 'node:assert/strict'
import test from 'node:test'
import {
  REIMBURSEMENT_STATUS,
  calculateReimbursementTotal,
  canDeleteReimbursement,
  canEditReimbursement,
  isOccurredDateInRange
} from './constants.ts'

test('reimbursement lifecycle only permits editable states', () => {
  assert.equal(canEditReimbursement(REIMBURSEMENT_STATUS.DRAFT), true)
  assert.equal(canEditReimbursement(REIMBURSEMENT_STATUS.REJECTED), true)
  assert.equal(canEditReimbursement(REIMBURSEMENT_STATUS.CANCELED), true)
  assert.equal(canEditReimbursement(REIMBURSEMENT_STATUS.PROCESSING), false)
  assert.equal(canEditReimbursement(REIMBURSEMENT_STATUS.APPROVED), false)
  assert.equal(canDeleteReimbursement(REIMBURSEMENT_STATUS.DRAFT), true)
  assert.equal(canDeleteReimbursement(REIMBURSEMENT_STATUS.DRAFT, 'process-1'), false)
  assert.equal(canDeleteReimbursement(REIMBURSEMENT_STATUS.REJECTED), false)
})

test('reimbursement total uses six-decimal business precision', () => {
  assert.equal(calculateReimbursementTotal([0.1, 0.2]), 0.3)
  assert.equal(calculateReimbursementTotal(['123.450000', 0.55]), 124)
  assert.equal(calculateReimbursementTotal([undefined, 1]), 1)
})

test('expense item date must be inside the declared range', () => {
  assert.equal(isOccurredDateInRange('2026-07-10', '2026-07-01', '2026-07-15'), true)
  assert.equal(isOccurredDateInRange('2026-07-20', '2026-07-01', '2026-07-15'), false)
  assert.equal(isOccurredDateInRange('2026-06-30', '2026-07-01', '2026-07-15'), false)
  assert.equal(isOccurredDateInRange('', '2026-07-01', '2026-07-15'), false)
  assert.equal(isOccurredDateInRange('2026-07-10', '', '2026-07-15'), false)
  assert.equal(isOccurredDateInRange('2026-07-10', '2026-07-01', ''), false)
})
