import assert from 'node:assert/strict'
import test from 'node:test'
import { filterReceivableCandidates } from './receivableCandidates.mjs'

const candidates = [
  {
    id: 1,
    no: 'HT-001',
    name: '年度服务',
    customerId: 10,
    customerName: '甲客户',
    totalPrice: 1000,
    totalReceivablePrice: 300,
    remainingReceivablePrice: 700
  },
  {
    id: 2,
    no: 'HT-002',
    name: '实施服务',
    customerId: 20,
    totalPrice: 500,
    totalReceivablePrice: 500,
    remainingReceivablePrice: 0
  }
]

test('candidate filtering supports all contracts and an optional customer', () => {
  assert.equal(filterReceivableCandidates(candidates), candidates)
  assert.deepEqual(filterReceivableCandidates(candidates, 10), [candidates[0]])
  assert.deepEqual(filterReceivableCandidates(candidates, 99), [])
})
