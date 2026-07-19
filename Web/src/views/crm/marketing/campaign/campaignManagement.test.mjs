import assert from 'node:assert/strict'
import test from 'node:test'
import { CampaignStatus, campaignActionVisibility, isEndAfterStart } from './campaignManagement.mjs'

test('draft supports maintenance but not terminal actions', () => {
  assert.deepEqual(campaignActionVisibility(CampaignStatus.DRAFT), {
    edit: true,
    start: true,
    delete: true,
    lock: false,
    terminate: false,
    complete: false
  })
})

test('active and locked statuses expose their lifecycle actions', () => {
  assert.deepEqual(campaignActionVisibility(CampaignStatus.ACTIVE), {
    edit: false,
    start: false,
    delete: false,
    lock: true,
    terminate: true,
    complete: true
  })
  assert.deepEqual(campaignActionVisibility(CampaignStatus.LOCKED), {
    edit: false,
    start: false,
    delete: false,
    lock: false,
    terminate: true,
    complete: true
  })
})

test('terminal and unknown statuses expose no mutations', () => {
  for (const status of [CampaignStatus.TERMINATED, CampaignStatus.COMPLETED, 999]) {
    assert.deepEqual(campaignActionVisibility(status), {
      edit: false,
      start: false,
      delete: false,
      lock: false,
      terminate: false,
      complete: false
    })
  }
})

test('campaign end time must be present and later than start time', () => {
  assert.equal(isEndAfterStart(1000, 1001), true)
  assert.equal(isEndAfterStart(1000, 1000), false)
  assert.equal(isEndAfterStart(1001, 1000), false)
  assert.equal(isEndAfterStart('', 1000), false)
  assert.equal(isEndAfterStart(1000, ''), false)
})
