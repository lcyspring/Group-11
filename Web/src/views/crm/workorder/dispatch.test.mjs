import assert from 'node:assert/strict'
import test from 'node:test'
import { canClaimWorkOrder, candidateLabel, normalizeCcUserIds } from './dispatch.ts'

test('only group members can claim pending unassigned orders', () => {
  assert.equal(canClaimWorkOrder(10, undefined, 3, 7, [7, 8]), true)
  assert.equal(canClaimWorkOrder(10, 9, 3, 7, [7, 8]), false)
  assert.equal(canClaimWorkOrder(20, undefined, 3, 7, [7, 8]), false)
  assert.equal(canClaimWorkOrder(10, undefined, 3, 6, [7, 8]), false)
})

test('cc ids are positive, unique and bounded', () => {
  assert.deepEqual(normalizeCcUserIds([2, 2, null, -1, 3, 4], 2), [2, 3])
})

test('candidate label exposes current open load', () => {
  assert.equal(candidateLabel('Alice', 4), 'Alice · 4')
})
