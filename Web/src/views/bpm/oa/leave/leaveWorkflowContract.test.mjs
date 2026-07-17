import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const create = await readFile(new URL('./create.vue', import.meta.url), 'utf8')
const list = await readFile(new URL('./index.vue', import.meta.url), 'utf8')

test('leave creation shows working days and enforces meaningful reason', () => {
  assert.match(create, /calculateWorkingDays/)
  assert.match(create, /workingDays/)
  assert.match(create, /min: 10, max: 200/)
  assert.match(create, /oa\.leave\.reasonLength/)
})

test('leave cancellation sends the Flowable process instance id', () => {
  assert.match(list, /cancelProcessInstanceByStartUser\(row\.processInstanceId, value\)/)
  assert.doesNotMatch(list, /cancelProcessInstanceByStartUser\(row\.id, value\)/)
})
