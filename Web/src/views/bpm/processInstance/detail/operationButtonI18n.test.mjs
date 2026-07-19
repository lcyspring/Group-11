import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./ProcessInstanceOperationButton.vue', import.meta.url), 'utf8')

test('built-in approval operations follow the active locale', () => {
  assert.match(source, /OperationButtonType\.APPROVE, 'approval\.approve'/)
  assert.match(source, /OperationButtonType\.REJECT, 'approval\.reject'/)
  assert.match(source, /builtInNames\.has\(configuredName\)/)
  assert.match(source, /t\(localeKeys\.get\(btnType\)/)
})
