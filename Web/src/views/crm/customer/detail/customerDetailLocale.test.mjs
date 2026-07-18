import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (relative) => readFileSync(new URL(relative, import.meta.url), 'utf8')
const customerDetail = read('./index.vue')
const permissionList = read('../../permission/components/PermissionList.vue')
const permissionForm = read('../../permission/components/PermissionForm.vue')
const transferForm = read('../../permission/components/TransferForm.vue')
const htmlEntry = read('../../../../../index.html')
const zhCn = read('../../../../locales/zh-CN/crm.ts')
const en = read('../../../../locales/en/crm.ts')
const ar = read('../../../../locales/ar/crm.ts')

test('customer lock action uses the explicit CRM customer translation key', () => {
  assert.match(customerDetail, /t\('crm\.customer\.lock'\)/)
  assert.doesNotMatch(customerDetail, /t\('lock'\)/)
  assert.match(zhCn, /lock: '锁定'/)
  assert.match(en, /lock: 'Lock'/)
  assert.match(ar, /lock: 'قفل'/)
})

test('customer team components use explicit CRM permission translation keys', () => {
  for (const source of [permissionList, permissionForm, transferForm]) {
    assert.doesNotMatch(source, /t\('permission\./)
  }
  assert.match(permissionList, /t\('crm\.permission\.addMember'\)/)
  assert.match(permissionForm, /t\('crm\.permission\.userName'\)/)
  assert.match(transferForm, /t\('crm\.permission\.newOwner'\)/)
  for (const locale of [zhCn, en, ar]) {
    assert.match(locale, /permission: \{/)
  }
})

test('the production entry declares the same Chinese locale used by a fresh session', () => {
  assert.match(htmlEntry, /<html lang="zh-CN">/)
  assert.doesNotMatch(htmlEntry, /<html lang="en">/)
})
