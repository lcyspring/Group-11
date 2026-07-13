import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateGrowthRate, formatGrowthRate } from './growthRate.ts'

test('calculates positive, negative and unchanged growth', () => {
  assert.equal(calculateGrowthRate(120, 100), 20)
  assert.equal(calculateGrowthRate(75, 100), -25)
  assert.equal(calculateGrowthRate(100, 100), 0)
})

test('treats zero or invalid baseline as not comparable', () => {
  assert.equal(calculateGrowthRate(10, 0), null)
  assert.equal(calculateGrowthRate('invalid', 10), null)
  assert.equal(formatGrowthRate(10, 0), '--')
})

test('accepts numeric strings and rounds to two decimal places', () => {
  assert.equal(calculateGrowthRate('2', '3'), -33.33)
})
