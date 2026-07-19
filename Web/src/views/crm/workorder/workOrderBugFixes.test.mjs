import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const view = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const assignForm = await readFile(new URL('./WorkOrderAssignForm.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../api/crm/workorder/index.ts', import.meta.url), 'utf8')
const controller = await readFile(
  new URL('../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/workorder/CrmWorkOrderController.java', import.meta.url),
  'utf8'
)
const service = await readFile(
  new URL('../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/workorder/CrmWorkOrderServiceImpl.java', import.meta.url),
  'utf8'
)
const migration = await readFile(
  new URL('../../../../../database/migrations/new-crm-work-order-export.sql', import.meta.url),
  'utf8'
)

test('work order list exports either filtered results or explicitly selected rows', () => {
  assert.match(view, /type="selection"/)
  assert.match(view, /selectedIds\.value\.length \? selectedIds\.value : undefined/)
  assert.match(view, /WorkOrderApi\.exportWorkOrders/)
  assert.match(api, /crm\/work-order\/export-excel/)
  assert.match(controller, /hasPermission\('crm:work-order:export'\)/)
  assert.match(controller, /reqVO\.setPageSize\(PAGE_SIZE_NONE\)/)
})

test('same handler is blocked only when the target group is also unchanged', () => {
  assert.match(assignForm, /isUnchangedAssignment\(currentGroupId, currentHandlerUserId, formData\.groupId, item\.id\)/)
  assert.match(service, /ObjectUtil\.equal\(old\.getGroupId\(\), targetGroupId\)[\s\S]*ObjectUtil\.equal\(old\.getHandlerUserId\(\), reqVO\.getHandlerUserId\(\)\)/)
})

test('export permission attaches to the service work order page instead of the statistics path collision', () => {
  assert.match(migration, /page\.component='crm\/workorder\/index'/)
  assert.match(migration, /COALESCE\(parent\.component,''\)<>'crm\/workorder\/index'/)
  assert.match(migration, /DELETE role_menu FROM system_role_menu/)
})

test('work order action menu is hidden when the row has no permitted action', () => {
  assert.match(view, /TableActions v-if="hasWorkOrderActions\(row\)" mode="menu"/)
  assert.match(view, /const hasWorkOrderActions = \(row: WorkOrderApi\.WorkOrderVO\)/)
  assert.match(view, /checkPermi\(\['crm:work-order:process'\]\)/)
})
