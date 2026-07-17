import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const mainList = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const embeddedList = await readFile(
  new URL('./components/ReceivableList.vue', import.meta.url),
  'utf8'
)

test('unsubmitted receivable drafts expose delete regardless of linked plan', () => {
  for (const source of [mainList, embeddedList]) {
    assert.match(source, /auditStatus === 0 && !scope\.row\.processInstanceId/)
    assert.doesNotMatch(source, /!scope\.row\.planId/)
    assert.match(source, /crm:receivable:delete/)
  }
})
