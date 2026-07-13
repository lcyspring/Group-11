import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeProductDealCycles } from './dealCycle.ts'

test('keeps deal cycle distinct from customer count and fills missing product name', () => {
  const result = normalizeProductDealCycles(
    [{ productName: null, customerDealCycle: 12.5, customerDealCount: 3 }],
    '未知产品'
  )

  assert.deepEqual(result, [
    { productName: '未知产品', customerDealCycle: 12.5, customerDealCount: 3 }
  ])
})
