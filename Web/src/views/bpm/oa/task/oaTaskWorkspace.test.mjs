import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../../api/bpm/oaTask/index.ts', import.meta.url), 'utf8')

test('OA task workspace exposes independent lifecycle operations', () => {
  assert.match(api, /\/bpm\/oa\/task\/create/)
  assert.match(api, /\/bpm\/oa\/task\/start/)
  assert.match(api, /\/bpm\/oa\/task\/complete/)
  assert.match(api, /new Date\(data\.dueTime\.replace\(' ', 'T'\)\)\.getTime\(\)/)
  assert.match(page, /row\.status === 0/)
  assert.match(page, /row\.status === 1/)
  assert.match(page, /oa\.task\.empty/)
  assert.match(page, /import dayjs from 'dayjs'/)
  assert.doesNotMatch(page, /v-if="row\.status < 2" link type="danger"/)
  assert.match(page, /flex flex-wrap gap-8px/)
})
