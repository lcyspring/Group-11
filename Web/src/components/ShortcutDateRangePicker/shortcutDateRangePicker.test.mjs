import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./index.vue', import.meta.url), 'utf8')

test('shortcut range state exists before setup invokes its initial change handler', () => {
  const declaration = source.indexOf('const shortcutDays = ref(7)')
  const mounted = source.indexOf('onMounted(')
  assert.ok(declaration > 0)
  assert.ok(mounted > declaration)
  assert.match(source, /v-model="shortcutDays"/)
  assert.match(source, /subtract\(shortcutDays\.value, 'd'\)/)
})

test('synchronous component emits do not create an unhandled promise chain', () => {
  assert.match(source, /const handleShortcutDaysChange = \(\) =>/)
  assert.match(source, /const emitDateRangePicker = \(\) =>/)
  assert.doesNotMatch(source, /await emitDateRangePicker/)
})
