import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')

test('event workspace offers day week and month ranges without reusing the edit range', () => {
  assert.match(source, /type ViewMode = 'day' \| 'week' \| 'month'/)
  assert.match(source, /date\.startOf\(viewMode\.value\)/)
  assert.match(source, /date\.endOf\(viewMode\.value\)/)
  assert.match(source, /const eventRange = ref/)
  assert.match(source, /form\.startTime = eventRange\.value\[0\]/)
})

test('calendar navigation reloads its selected period', () => {
  assert.match(source, /const movePeriod = async/)
  assert.match(source, /const goToday = async/)
  assert.match(source, /await changeView\(\)/)
})
