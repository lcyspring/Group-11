import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateWorkingDays } from './workingDays.mjs'

const at = (text) => new Date(`${text}T09:00:00`).getTime()

test('same weekday counts as one leave day', () => {
  assert.equal(calculateWorkingDays(at('2026-07-17'), at('2026-07-17') + 60 * 60 * 1000), 1)
})

test('weekend is excluded from an inclusive range', () => {
  assert.equal(calculateWorkingDays(at('2026-07-17'), at('2026-07-20')), 2)
})

test('invalid or reversed time range is zero', () => {
  assert.equal(calculateWorkingDays(at('2026-07-20'), at('2026-07-17')), 0)
})
