import assert from 'node:assert/strict'
import test from 'node:test'
import { buildWorkOrderTrendOptions } from './workOrderTrendOptions.ts'

const labels = { created: '新建', completed: '完结' }

test('builds a new reactive chart option from work-order trend rows', () => {
  const empty = buildWorkOrderTrendOptions([], labels)
  const populated = buildWorkOrderTrendOptions(
    [
      { time: '2026-07-17', createdCount: 3, completedCount: 1 },
      { time: '2026-07-18', createdCount: 5, completedCount: 4 }
    ],
    labels
  )

  assert.notStrictEqual(populated, empty)
  assert.deepEqual(empty.xAxis.data, [])
  assert.deepEqual(populated.xAxis.data, ['2026-07-17', '2026-07-18'])
  assert.deepEqual(populated.series[0].data, [3, 5])
  assert.deepEqual(populated.series[1].data, [1, 4])
})
