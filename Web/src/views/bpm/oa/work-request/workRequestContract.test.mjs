import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
const api = readFileSync(new URL('../../../../api/bpm/oaWorkRequest/index.ts', import.meta.url), 'utf8')
const page = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
test('work request exposes create and list workflow endpoints', () => { assert.match(api, /work-request\/create/); assert.match(api, /work-request\/list/); assert.match(page, /oa.workRequest/) })
test('work request renders governed status and useful lifecycle actions', () => {
  assert.match(page, /BPM_PROCESS_INSTANCE_STATUS/)
  assert.match(page, /dict-tag/)
  assert.match(page, /showDetail/)
  assert.match(page, /showProgress/)
  assert.match(page, /cancelProcessInstanceByStartUser/)
  assert.match(page, /row\.status === 1/)
  assert.match(page, /approvedTime/)
  assert.match(page, /onActivated/)
  assert.doesNotMatch(page, /<el-table-column prop="status"[^>]*\/>/)
})
