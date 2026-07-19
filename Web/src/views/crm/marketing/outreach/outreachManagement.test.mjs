import assert from 'node:assert/strict'
import test from 'node:test'
import {
  BroadcastStatus,
  DeliveryStatus,
  RecipientStatus,
  broadcastActionVisibility,
  channelNeedsEmail,
  channelNeedsSms,
  hasTargets,
  isValidTemplateParams,
  formatMetricRate
} from './outreachManagement.mjs'

test('action matrix exposes every lifecycle command only in its valid state', () => {
  assert.deepEqual(broadcastActionVisibility(BroadcastStatus.DRAFT), {
    edit: true, delete: true, submit: true, approval: false, send: false, retry: false, recipients: true
  })
  assert.equal(broadcastActionVisibility(BroadcastStatus.REJECTED).edit, true)
  assert.equal(broadcastActionVisibility(BroadcastStatus.PENDING_REVIEW).approval, true)
  assert.equal(broadcastActionVisibility(BroadcastStatus.READY).send, true)
  assert.equal(broadcastActionVisibility(BroadcastStatus.PARTIAL_FAILED).retry, true)
  assert.equal(broadcastActionVisibility(BroadcastStatus.SENT).send, false)
  for (const status of Object.values(BroadcastStatus)) {
    const actions = broadcastActionVisibility(status)
    assert.equal(actions.recipients, true)
    assert.equal(typeof actions.delete, 'boolean')
  }
})

test('channel requirements distinguish sms, email and dual channel', () => {
  assert.equal(channelNeedsSms(1), true)
  assert.equal(channelNeedsEmail(1), false)
  assert.equal(channelNeedsSms(2), false)
  assert.equal(channelNeedsEmail(2), true)
  assert.equal(channelNeedsSms(3), true)
  assert.equal(channelNeedsEmail(3), true)
})

test('template parameters accept only a JSON object', () => {
  assert.equal(isValidTemplateParams(''), true)
  assert.equal(isValidTemplateParams(undefined), true)
  assert.equal(isValidTemplateParams('   '), true)
  assert.equal(isValidTemplateParams('{"name":"Alice"}'), true)
  assert.equal(isValidTemplateParams('null'), false)
  assert.equal(isValidTemplateParams('[]'), false)
  assert.equal(isValidTemplateParams('1'), false)
  assert.equal(isValidTemplateParams('{broken'), false)
})

test('at least one customer or contact is required', () => {
  assert.equal(hasTargets([], []), false)
  assert.equal(hasTargets(undefined, undefined), false)
  assert.equal(hasTargets([1], []), true)
  assert.equal(hasTargets([], [2]), true)
})

test('recipient status values match the backend delivery contract', () => {
  assert.deepEqual(RecipientStatus, {
    PENDING: 10, SENDING: 15, SENT: 20, FAILED: 30, SUPPRESSED: 40, RECORDED: 50
  })
})

test('provider result states and percentages preserve the metric contract', () => {
  assert.deepEqual(DeliveryStatus, {
    UNKNOWN: 0, PROVIDER_PENDING: 10, DELIVERED: 20, FAILED: 30, ACCEPTED: 40
  })
  assert.equal(formatMetricRate(81.5), '81.50%')
  assert.equal(formatMetricRate(undefined), '0.00%')
})
