import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (relative) => readFileSync(new URL(relative, import.meta.url), 'utf8')
const customerView = read('./index.vue')
const dialog = read('./CustomerExportTaskDialog.vue')
const api = read('../../../api/crm/customer/index.ts')
const controller = read(
  '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/exporttask/CrmExportTaskController.java'
)
const service = read(
  '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/exporttask/CrmExportTaskServiceImpl.java'
)
const mapper = read(
  '../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/exporttask/CrmExportTaskMapper.java'
)
const migration = read('../../../../../database/migrations/new-crm-export-task.sql')
const applicationYaml = read('../../../../../Server/mitedtsm-server/src/main/resources/application.yaml')

test('customer export submits a background task and exposes its task center', () => {
  assert.match(api, /\/crm\/export-task\/customer/)
  assert.match(api, /\/crm\/export-task\/page/)
  assert.match(api, /\/crm\/export-task\/download-token/)
  assert.match(api, /\/crm\/export-task\/download/)
  assert.match(customerView, /createCustomerExportTask/)
  assert.match(customerView, /CustomerExportTaskDialog/)
  assert.doesNotMatch(customerView, /CustomerApi\.exportCustomer/)
})

test('server freezes filter and object snapshots and uses five explicit states', () => {
  assert.match(service, /setFilterSnapshot\(JsonUtils\.toJsonString\(filter\)\)/)
  assert.match(service, /setObjectIdsSnapshot\(JsonUtils\.toJsonString\(objectIds\)\)/)
  for (const state of ['QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'EXPIRED']) {
    assert.match(service, new RegExp(`${state}\\.getStatus\\(\\)`))
  }
  assert.match(migration, /object_ids_snapshot/)
  assert.match(migration, /download_token_hash/)
})

test('download is permission-revalidated and protected by an atomic one-time token', () => {
  assert.match(controller, /@PreAuthorize\("@ss\.hasPermission\('crm:customer:export'\)"\)/)
  assert.match(service, /validateSnapshot\(task\)/)
  assert.match(service, /sha256\(token\)/)
  assert.match(mapper, /getDownloadTokenHash, hash/)
  assert.match(mapper, /getDownloadTokenExpiresAt, now/)
  assert.match(mapper, /getDownloadTokenHash, null/)
})

test('task UI polls active work and downloads only after token issuance', () => {
  assert.match(dialog, /hasActiveTask/)
  assert.match(dialog, /window\.setTimeout/)
  assert.match(dialog, /issueExportDownloadToken/)
  assert.match(dialog, /downloadExportTask/)
  assert.match(dialog, /onBeforeUnmount\(stopPolling\)/)
})

test('runtime limits are explicit YAML configuration', () => {
  for (const key of [
    'batch-size', 'max-batch-size', 'max-pending-per-user', 'max-rows',
    'retention-hours', 'token-ttl-seconds', 'lock-key', 'lock-lease-seconds'
  ]) {
    assert.match(applicationYaml, new RegExp(key))
  }
})
