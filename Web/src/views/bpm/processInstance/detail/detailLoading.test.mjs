import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const detail = fs.readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const migration = fs.readFileSync(
  new URL('../../../../../../database/maintenance/repair/repair-crm-orphan-approval-state.sql', import.meta.url),
  'utf8'
)

test('approval detail loads before the model view and rejects empty ids locally', () => {
  assert.match(detail, /if \(!props\.id\)/)
  assert.match(detail, /const loaded = await getApprovalDetail\(\)/)
  assert.match(detail, /if \(loaded\) \{\s*await getProcessModelView\(\)/)
  assert.doesNotMatch(detail, /getApprovalDetail\(\)\s*\n\s*\/\/[^\n]*\n\s*getProcessModelView\(\)/)
})

test('approval and model request failures are handled without rejected lifecycle promises', () => {
  assert.match(detail, /const getApprovalDetail = async \(\) => \{[\s\S]*?catch \{[\s\S]*?return false/)
  assert.match(detail, /const getProcessModelView = async \(\) => \{[\s\S]*?catch \{/)
  assert.match(detail, /const refresh = \(\) => \{[\s\S]*?void getDetail\(\)/)
  assert.match(detail, /onMounted\(async \(\) => \{\s*await getDetail\(\)/)
})

test('orphan repair is audited and covers every CRM approval aggregate', () => {
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `crm_approval_repair_record`/)
  for (const table of [
    'crm_contract',
    'crm_receivable',
    'crm_receivable_refund',
    'crm_reimbursement',
    'crm_contract_amendment'
  ]) {
    assert.match(migration, new RegExp(`FROM ${table} source`))
    assert.match(migration, new RegExp(`UPDATE ${table} source`))
  }
  assert.match(migration, /LEFT JOIN ACT_HI_PROCINST flow/)
  assert.match(migration, /source\.audit_status=10/)
  assert.match(migration, /source\.audit_status=40,source\.process_instance_id=NULL/)
})
