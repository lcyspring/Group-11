import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./CustomerImportForm.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/customer/index.ts', import.meta.url), 'utf8')
const controller = readFileSync(new URL(
  '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/customer/CrmCustomerController.java',
  import.meta.url
), 'utf8')

test('customer import is previewed before a separate confirmation command', () => {
  assert.match(api, /\/crm\/customer\/import-preview'/)
  assert.match(api, /\/crm\/customer\/import-preview\/confirm'/)
  assert.match(view, /runPreview/)
  assert.match(view, /confirmImport/)
  assert.match(view, /fieldMapping/)
  assert.match(view, /preview\.rows/)
})

test('server protects preview and confirmation with the import permission', () => {
  assert.match(controller, /@PostMapping\("\/import-preview"\)/)
  assert.match(controller, /@PostMapping\("\/import-preview\/confirm"\)/)
  assert.equal((controller.match(/crm:customer:import/g) || []).length >= 3, true)
})

test('failed preview rows can be downloaded for correction', () => {
  assert.match(view, /downloadFailureDetails/)
  assert.match(view, /row\.action === 'FAILURE'/)
  assert.match(view, /application\/json/)
})
