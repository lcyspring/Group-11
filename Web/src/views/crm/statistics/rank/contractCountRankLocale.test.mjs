import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./components/ContractCountRank.vue', import.meta.url), 'utf8')
const zh = readFileSync(new URL('../../../../locales/zh-CN/crm.ts', import.meta.url), 'utf8')

test('contract count rank resolves its table and chart axis label in rank scope', () => {
  assert.match(view, /t\('rank\.contractCount2'\)/)
  assert.match(zh, /rank:\s*\{[\s\S]*contractCount2: '合同数量'/)
})
