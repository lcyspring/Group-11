import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const createPage = readFileSync(new URL('./create.vue', import.meta.url), 'utf8')
const listPage = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/customerVisit/index.ts', import.meta.url), 'utf8')
const router = readFileSync(new URL('../../../router/modules/remaining.ts', import.meta.url), 'utf8')

test('customer visit starts governed approval with customer and contact context', () => {
  assert.match(createPage, /getProcessDefinition\(undefined, 'crm_customer_visit_audit'\)/)
  assert.match(createPage, /getContactPageByCustomer/)
  assert.match(createPage, /startUserSelectAssignees/)
  assert.match(api, /\/crm\/customer-visit\/create/)
})

test('approved visit exposes result recording and creates a follow-up through the backend', () => {
  assert.match(listPage, /row\.auditStatus === 2 && row\.resultStatus === 0/)
  assert.match(listPage, /recordCustomerVisitResult/)
  assert.match(api, /\/crm\/customer-visit\/result/)
  assert.match(api, /followUpRecordId/)
})

test('visit pages have stable hidden routes and process progress', () => {
  assert.match(router, /name: 'CrmCustomerVisitCreate'/)
  assert.match(router, /name: 'CrmCustomerVisitDetail'/)
  assert.match(router, /activeMenu: '\/crm\/customer-visit'/)
  assert.match(listPage, /BpmProcessInstanceDetail/)
})

test('cached visit list reloads after returning from create page', () => {
  assert.match(listPage, /let initialized = false/)
  assert.match(listPage, /initialized = true/)
  assert.match(listPage, /onActivated\(\(\) => \{ if \(initialized\) load\(\) \}\)/)
})
