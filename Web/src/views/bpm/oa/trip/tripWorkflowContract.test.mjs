import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('./create.vue', import.meta.url), 'utf8')
const list = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../../api/bpm/trip/index.ts', import.meta.url), 'utf8')
const router = readFileSync(new URL('../../../../router/modules/remaining.ts', import.meta.url), 'utf8')
const reimbursement = readFileSync(new URL('../../../crm/reimbursement/ReimbursementForm.vue', import.meta.url), 'utf8')

test('trip creation covers prototype fields and starts oa_trip approval', () => {
  for (const field of ['startTime', 'endTime', 'destination', 'reason', 'estimatedExpense', 'companionUserIds', 'attachmentUrls']) {
    assert.match(page, new RegExp(field))
  }
  assert.match(page, /getProcessDefinition\(undefined, 'oa_trip'\)/)
  assert.match(page, /startUserSelectAssignees/)
  assert.match(api, /\/bpm\/oa\/trip\/create/)
})

test('trip pages expose details, process progress and stable hidden routes', () => {
  assert.match(list, /BpmProcessInstanceDetail/)
  assert.match(router, /name: 'OATripCreate'/)
  assert.match(router, /name: 'OATripDetail'/)
  assert.match(router, /activeMenu: '\/bpm\/oa\/trip'/)
})

test('approved completed trips can be explicitly linked to reimbursement', () => {
  assert.match(api, /reimbursable-list/)
  assert.match(reimbursement, /getReimbursableTrips/)
  assert.match(reimbursement, /tripId/)
  assert.match(reimbursement, /tripHint/)
})
