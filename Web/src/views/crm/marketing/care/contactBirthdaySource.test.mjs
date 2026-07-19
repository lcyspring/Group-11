import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const contactForm = await readFile(new URL('../../contact/ContactForm.vue', import.meta.url), 'utf8')
const contactApi = await readFile(new URL('../../../../api/crm/contact/index.ts', import.meta.url), 'utf8')
const customerForm = await readFile(new URL('../../customer/CustomerForm.vue', import.meta.url), 'utf8')
const customerList = await readFile(new URL('../../customer/index.vue', import.meta.url), 'utf8')
const customerDetail = await readFile(new URL('../../customer/detail/CustomerDetailsInfo.vue', import.meta.url), 'utf8')
const customerApi = await readFile(new URL('../../../../api/crm/customer/index.ts', import.meta.url), 'utf8')
const marketingApi = await readFile(new URL('../../../../api/crm/marketing/index.ts', import.meta.url), 'utf8')
const carePage = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const locales = await Promise.all(['zh-CN', 'en', 'ar'].map((locale) =>
  readFile(new URL(`../../../../locales/${locale}/crm.ts`, import.meta.url), 'utf8')))

test('contact create and update form exposes the birthday source used by customer care', () => {
  assert.match(contactForm, /prop="birthday"/)
  assert.match(contactForm, /v-model="formData\.birthday"/)
  assert.match(contactForm, /value-format="YYYY-MM-DD"/)
  assert.match(contactForm, /birthday: undefined/)
  assert.match(contactApi, /birthday\?: string/)
})

test('customer create and update form exposes its own birthday source', () => {
  assert.match(customerForm, /prop="birthday"/)
  assert.match(customerForm, /v-model="formData\.birthday"/)
  assert.match(customerForm, /value-format="YYYY-MM-DD"/)
  assert.match(customerForm, /birthday: undefined/)
  assert.match(customerList, /:label="t\('birthday'\)" prop="birthday"/)
  assert.match(customerDetail, /customer\.birthday \|\| '-'/)
  assert.match(customerApi, /birthday\?: string/)
  assert.match(marketingApi, /contactId\?: number/)
  assert.match(carePage, /birthdayQuery\.targetType/)
  assert.match(carePage, /customerBirthday/)
  assert.match(carePage, /contactBirthday/)
})

test('all locales identify both customer and contact birthday sources', () => {
  for (const locale of locales) assert.match(locale, /birthdayPlaceholder:/)
  assert.match(locales[0], /upcomingBirthdays: '客户与联系人生日'/)
  assert.match(locales[1], /upcomingBirthdays: 'Customer and Contact Birthdays'/)
  assert.match(locales[2], /upcomingBirthdays: 'أعياد ميلاد العملاء وجهات الاتصال'/)
})
