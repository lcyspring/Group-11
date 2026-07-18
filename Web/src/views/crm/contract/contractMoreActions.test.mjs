import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const page = await readFile(new URL('./index.vue', import.meta.url), 'utf8')

test('contract more menu is hidden when the row has no permitted actions', () => {
  assert.match(page, /:show-more="hasContractMoreActions\(scope\.row\)"/)
  assert.match(page, /const hasContractMoreActions =/)
  assert.match(page, /checkPermi\(\['crm:contract:update'\]\)/)
  assert.match(page, /checkPermi\(\['crm:contract:delete'\]\)/)
})

test('contract menu keeps edit, submit, approval and delete state guards', () => {
  assert.match(page, /\[0, 30, 40\]\.includes\(scope\.row\.auditStatus\)/)
  assert.match(page, /scope\.row\.auditStatus === 0/)
  assert.match(page, /scope\.row\.processInstanceId/)
  assert.match(page, /!scope\.row\.processInstanceId/)
})
