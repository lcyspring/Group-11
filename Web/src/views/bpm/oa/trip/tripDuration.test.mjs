import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateTripDays, isFutureTrip } from './tripDuration.mjs'

test('trip duration keeps hour precision and rounds upward to two decimals', () => {
  const start = Date.parse('2026-08-01T08:00:00+08:00')
  assert.equal(calculateTripDays(start, start + 36 * 60 * 60 * 1000), 1.5)
  assert.equal(calculateTripDays(start, start + 60 * 60 * 1000), 0.05)
})

test('invalid and historical trips are rejected by the client contract', () => {
  assert.equal(calculateTripDays(20, 10), 0)
  assert.equal(isFutureTrip(10, 20), false)
  assert.equal(isFutureTrip(30, 20), true)
})
