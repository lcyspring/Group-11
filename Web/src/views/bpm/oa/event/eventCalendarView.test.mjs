import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const form = readFileSync(new URL('./OaEventForm.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../../api/bpm/oaEvent/index.ts', import.meta.url), 'utf8')

test('event workspace offers day week and month ranges without reusing the edit range', () => {
  assert.match(source, /type ViewMode = 'day' \| 'week' \| 'month'/)
  assert.match(source, /date\.startOf\(viewMode\.value\)/)
  assert.match(source, /date\.endOf\(viewMode\.value\)/)
  assert.match(source, /const range = ref/)
  assert.match(source, /await Api\.getOaEventList\(range\.value\[0\], range\.value\[1\]\)/)
})

test('event API converts display timestamps to Spring LocalDateTime ISO format', () => {
  assert.match(api, /value\.replace\(' ', 'T'\)/)
  assert.match(api, /new Date\(toLocalDateTime\(data\.startTime\)\)\.getTime\(\)/)
  assert.match(source, /oa\.event\.empty/)
})

test('event page opens the same dialog from toolbar empty state and edit action', () => {
  assert.match(source, /import dayjs from 'dayjs'/)
  assert.match(source, /<OaEventForm ref="formRef" @success="load"/)
  assert.equal(source.match(/@click="openForm\('create'\)"/g)?.length, 2)
  assert.match(source, /@click="openForm\('update', row\)"/)
  assert.doesNotMatch(source, /focusCreate|document\.querySelector/)
})

test('event dialog validates and submits explicit create and update payloads', () => {
  assert.match(form, /<Dialog v-model="dialogVisible"/)
  assert.match(form, /type="datetimerange"/)
  assert.match(form, /timeRangeRequired/)
  assert.match(form, /timeRangeInvalid/)
  assert.match(form, /Api\.createOaEvent\(payload\)/)
  assert.match(form, /Api\.updateOaEvent\(\{ \.\.\.payload, id: formData\.id \}\)/)
  assert.match(form, /emit\('success'\)/)
})

test('calendar navigation reloads its selected period', () => {
  assert.match(source, /const movePeriod = async/)
  assert.match(source, /const goToday = async/)
  assert.match(source, /await changeView\(\)/)
})
