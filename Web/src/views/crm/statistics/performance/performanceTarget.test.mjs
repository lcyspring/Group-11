import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
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

test('target pages contain handled failure states and explicit retry actions', () => {
  const management = readFileSync(
    new URL('./components/PerformanceTargetManagement.vue', import.meta.url),
    'utf8'
  )
  const completion = readFileSync(
    new URL('./components/TargetCompletionPerformance.vue', import.meta.url),
    'utf8'
  )

  for (const source of [management, completion]) {
    assert.match(source, /v-if="loadFailed"/)
    assert.match(source, /performance\.targetLoadFailed/)
    assert.match(source, /performance\.targetRetry/)
    assert.match(source, /catch \{[\s\S]*?loadFailed\.value = true/)
  }
})
