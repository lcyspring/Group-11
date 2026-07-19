import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import zhCN from '../../locales/zh-CN/common.ts'
import en from '../../locales/en/common.ts'
import ar from '../../locales/ar/common.ts'

const source = new TextDecoder('utf-8', { fatal: true }).decode(
  readFileSync(new URL('./remaining.ts', import.meta.url))
)

test('hidden route tags keep translation keys instead of startup-locale text', () => {
  assert.doesNotMatch(source, /\bt\s*\(/, 'remaining routes must not translate titles at module load')
  const keys = [...source.matchAll(/title: 'common\.router\.([^']+)'/g)].map((match) => match[1])
  assert.equal(keys.length, 45)
  for (const key of keys) {
    assert.equal(typeof zhCN.router[key], 'string', `missing zh-CN route title: ${key}`)
    assert.equal(typeof en.router[key], 'string', `missing en route title: ${key}`)
    assert.equal(typeof ar.router[key], 'string', `missing ar route title: ${key}`)
  }
})

test('approval detail tag has explicit titles in all supported languages', () => {
  assert.equal(zhCN.router.bpmProcessInstanceDetail, '流程详情')
  assert.equal(en.router.bpmProcessInstanceDetail, 'Process Detail')
  assert.equal(ar.router.bpmProcessInstanceDetail, 'تفاصيل العملية')
})
