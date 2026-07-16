import assert from 'node:assert/strict'
import test from 'node:test'
import { canMobileCheckIn, slaState } from './governance.ts'

test('mobile check-in requires an open order with both coordinates', () => {
  assert.equal(canMobileCheckIn({ status: 20, serviceLatitude: 31.2, serviceLongitude: 121.5 }), true)
  assert.equal(canMobileCheckIn({ status: 30, serviceLatitude: 31.2, serviceLongitude: 121.5 }), false)
  assert.equal(canMobileCheckIn({ status: 20, serviceLatitude: 31.2 }), false)
})

test('SLA state has deterministic precedence', () => {
  assert.equal(slaState(), 'NONE')
  assert.equal(slaState({ paused: true, overdue: true }), 'PAUSED')
  assert.equal(slaState({ paused: false, overdue: true }), 'OVERDUE')
  assert.equal(slaState({ paused: false, overdue: false, escalatedAt: new Date() }), 'ESCALATED')
  assert.equal(slaState({ paused: false, overdue: false, status: 2 }), 'COMPLETED')
  assert.equal(slaState({ paused: false, overdue: false, status: 0 }), 'ACTIVE')
})
