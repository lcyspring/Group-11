import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./ProcessInstanceOperationButton.vue', import.meta.url), 'utf8')

test('handled approval tasks disappear immediately and cannot return from a stale refresh', () => {
  assert.match(source, /const handledTaskIds = new Set<string>\(\)/)
  assert.match(source, /await TaskApi\.approveTask\(data\)[\s\S]*?markCurrentTaskHandled\(\)/)
  assert.match(source, /await TaskApi\.rejectTask\(data\)[\s\S]*?markCurrentTaskHandled\(\)/)
  assert.match(source, /handledTaskIds\.add\(String\(runningTask\.value\.id\)\)/)
  assert.match(
    source,
    /runningTask\.value = task && !handledTaskIds\.has\(String\(task\.id\)\) \? task : undefined/
  )
})
