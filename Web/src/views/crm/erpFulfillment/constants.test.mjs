import assert from 'node:assert/strict'
import test from 'node:test'
import {
  FULFILLMENT_STATUS,
  canCreateOrRetryFulfillment,
  canRefreshFulfillment,
  fulfillmentStatusType,
  progressPercent
} from './constants.ts'

test('fulfillment actions follow persisted state', () => {
  assert.equal(canCreateOrRetryFulfillment(true), true)
  assert.equal(canCreateOrRetryFulfillment(true, FULFILLMENT_STATUS.FAILED), true)
  assert.equal(canCreateOrRetryFulfillment(true, FULFILLMENT_STATUS.CREATED), false)
  assert.equal(canCreateOrRetryFulfillment(false), false)
  assert.equal(canRefreshFulfillment(FULFILLMENT_STATUS.CREATED, 8), true)
  assert.equal(canRefreshFulfillment(FULFILLMENT_STATUS.CREATED), false)
  assert.equal(canRefreshFulfillment(FULFILLMENT_STATUS.FAILED, 8), false)
})

test('progress is bounded and precise', () => {
  assert.equal(progressPercent(2.5, 10), 25)
  assert.equal(progressPercent(11, 10), 100)
  assert.equal(progressPercent(-1, 10), 0)
  assert.equal(progressPercent(1, 0), 0)
})

test('status tags expose creation outcome', () => {
  assert.equal(fulfillmentStatusType(FULFILLMENT_STATUS.CREATED), 'success')
  assert.equal(fulfillmentStatusType(FULFILLMENT_STATUS.FAILED), 'danger')
  assert.equal(fulfillmentStatusType(FULFILLMENT_STATUS.CREATING), 'warning')
})
