import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./ProcessInstanceOperationButton.vue', import.meta.url), 'utf8')
const zh = readFileSync(new URL('../../../../locales/zh-CN/bpm.ts', import.meta.url), 'utf8')
const en = readFileSync(new URL('../../../../locales/en/bpm.ts', import.meta.url), 'utf8')
const ar = readFileSync(new URL('../../../../locales/ar/bpm.ts', import.meta.url), 'utf8')

test('approval opinion interpolation has a localized approval node name', () => {
  assert.match(source, /t\('approval\.approval'\)/)
  assert.match(zh, /approval:\s*\{[\s\S]*approval: '审批'/)
  assert.match(en, /approval:\s*\{[\s\S]*approval: 'Approval'/)
  assert.match(ar, /approval:\s*\{[\s\S]*approval: 'موافقة'/)
})
