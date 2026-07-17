import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
const api = readFileSync(new URL('../../../../api/bpm/oaWorkRequest/index.ts', import.meta.url), 'utf8')
const page = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
test('work request exposes create and list workflow endpoints', () => { assert.match(api, /work-request\/create/); assert.match(api, /work-request\/list/); assert.match(page, /oa.workRequest/) })
