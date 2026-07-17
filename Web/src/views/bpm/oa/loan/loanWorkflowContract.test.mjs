import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const createPage = readFileSync(new URL('./create.vue', import.meta.url), 'utf8')
const listPage = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const detailPage = readFileSync(new URL('./detail.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../../api/bpm/loan/index.ts', import.meta.url), 'utf8')
const router = readFileSync(new URL('../../../../router/modules/remaining.ts', import.meta.url), 'utf8')

test('loan creation uses the managed process and governed position limit', () => {
  assert.match(createPage, /getProcessDefinition\(undefined, 'oa_loan'\)/)
  assert.match(createPage, /getMyLimit\(\)/)
  assert.match(createPage, /escalatedApproval/)
  assert.match(createPage, /startUserSelectAssignees/)
  assert.match(api, /\/bpm\/oa\/loan\/create/)
})

test('loan routes and pages expose approval progress and repayment lifecycle', () => {
  assert.match(router, /name: 'OALoanCreate'/)
  assert.match(router, /name: 'OALoanDetail'/)
  assert.match(router, /activeMenu: '\/bpm\/oa\/loan'/)
  assert.match(listPage, /BpmProcessInstanceDetail/)
  assert.match(listPage, /createRepayment/)
  assert.match(detailPage, /getRepayments/)
  assert.match(detailPage, /repaymentRecords/)
  assert.match(listPage, /onActivated\(\(\) => \{ if \(initialized\) load\(\) \}/)
})

test('repayment API is explicit and separate from loan approval', () => {
  assert.match(api, /\/bpm\/oa\/loan\/repayment\/create/)
  assert.match(api, /\/bpm\/oa\/loan\/repayment\/list/)
  assert.match(api, /outstandingAmount/)
  assert.match(api, /repaymentStatus/)
})
