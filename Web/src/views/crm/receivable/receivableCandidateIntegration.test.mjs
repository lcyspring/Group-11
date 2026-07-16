import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const form = await readFile(new URL('./ReceivableForm.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../api/crm/contract/index.ts', import.meta.url), 'utf8')
const controller = await readFile(
  new URL(
    '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/contract/CrmContractController.java',
    import.meta.url
  ),
  'utf8'
)
const service = await readFile(
  new URL(
    '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/contract/CrmContractServiceImpl.java',
    import.meta.url
  ),
  'utf8'
)

test('candidate endpoint is explicit and protected by receivable create permission', () => {
  assert.match(api, /\/crm\/contract\/receivable-candidates/)
  assert.match(controller, /@GetMapping\("\/receivable-candidates"\)/)
  assert.match(controller, /hasPermission\('crm:receivable:create'\)/)
  assert.match(controller, /required = false/)
})

test('candidate backend limits ordinary users to direct owner or write access', () => {
  assert.match(service, /isCrmAdmin\(userId\)/)
  assert.match(service, /CrmPermissionLevelEnum\.isOwner/)
  assert.match(service, /CrmPermissionLevelEnum\.isWrite/)
  assert.match(service, /contract\.getOwnerUserId\(\), userId/)
  assert.match(controller, /getReservedPriceMapByContractId/)
})

test('receivable form supports contract-first selection and disables exhausted contracts', () => {
  assert.match(form, /:disabled="formType !== 'create'"/)
  assert.doesNotMatch(form, /formType !== 'create' \|\| !formData\.customerId/)
  assert.match(form, /:disabled="data\.remainingReceivablePrice <= 0"/)
  assert.match(form, /formData\.value\.customerId = contract\.customerId/)
  assert.match(form, /formData\.value\.price = contract\.remainingReceivablePrice/)
})
