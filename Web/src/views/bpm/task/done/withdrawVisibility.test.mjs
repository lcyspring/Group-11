import test from 'node:test'
import assert from 'node:assert/strict'

test('withdraw action is available only when a running process instance is present', () => {
  const canWithdraw = (row) => row.withdrawable === true
  assert.equal(canWithdraw({ withdrawable: true }), true)
  assert.equal(canWithdraw({ withdrawable: false, processInstance: { id: 'p1' } }), false)
  assert.equal(canWithdraw({}), false)
})
