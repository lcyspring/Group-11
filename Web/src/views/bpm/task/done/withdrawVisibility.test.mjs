import test from 'node:test'
import assert from 'node:assert/strict'

test('withdraw action is available only when a running process instance is present', () => {
  const canWithdraw = (row) => Boolean(row.processInstance)
  assert.equal(canWithdraw({ processInstance: { id: 'p1' } }), true)
  assert.equal(canWithdraw({ processInstance: null }), false)
  assert.equal(canWithdraw({}), false)
})
