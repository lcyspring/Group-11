import assert from 'node:assert/strict'
import test from 'node:test'
import { formatGrowthRate } from './growthRate.ts'

test('preserves the precise server-side growth rate', () => {
  assert.equal(formatGrowthRate('20.00'), '20.00')
  assert.equal(formatGrowthRate(-25), -25)
  assert.equal(formatGrowthRate('900719925474099200.00'), '900719925474099200.00')
})

test('renders a missing server-side rate as not comparable', () => {
  assert.equal(formatGrowthRate(null), '--')
  assert.equal(formatGrowthRate(undefined), '--')
})
