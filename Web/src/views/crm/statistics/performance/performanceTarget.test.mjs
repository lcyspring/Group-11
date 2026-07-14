import assert from 'node:assert/strict'
import test from 'node:test'
import { buildQuarterTargets, isValidTargetValue, sumTargetValues } from './performanceTarget.ts'

test('validates amount and count target formats independently', () => {
  assert.equal(isValidTargetValue('100.25', false), true)
  assert.equal(isValidTargetValue('100.25', true), false)
  assert.equal(isValidTargetValue('-1', false), false)
  assert.equal(isValidTargetValue('1.234', false), false)
})

test('sums large decimal targets without Number precision loss', () => {
  assert.equal(sumTargetValues(['9007199254740991.99', '0.01'], false), '9007199254740992.00')
})

test('derives four quarters from twelve monthly targets', () => {
  assert.deepEqual(
    buildQuarterTargets(['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'], true),
    ['6', '15', '24', '33']
  )
})
