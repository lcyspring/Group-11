import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../../api/bpm/oaEvent/index.ts', import.meta.url), 'utf8')

test('event workspace offers day week and month ranges without reusing the edit range', () => {
  assert.match(source, /type ViewMode = 'day' \| 'week' \| 'month'/)
  assert.match(source, /date\.startOf\(viewMode\.value\)/)
  assert.match(source, /date\.endOf\(viewMode\.value\)/)
  assert.match(source, /const eventRange = ref/)
  assert.match(source, /form\.startTime = eventRange\.value\[0\]/)
})

test('event API converts display timestamps to Spring LocalDateTime ISO format', () => {
  assert.match(api, /value\.replace\(' ', 'T'\)/)
  assert.match(api, /new Date\(toLocalDateTime\(data\.startTime\)\)\.getTime\(\)/)
  assert.match(source, /oa\.event\.empty/)
})

test('event page explicitly imports dayjs for runtime setup', () => {
  assert.match(source, /import dayjs from 'dayjs'/)
  assert.match(source, /oa\.event\.create/)
  assert.doesNotMatch(source, /common\.create/)
})

test('calendar navigation reloads its selected period', () => {
  assert.match(source, /const movePeriod = async/)
  assert.match(source, /const goToday = async/)
  assert.match(source, /await changeView\(\)/)
})
