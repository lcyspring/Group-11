import assert from 'node:assert/strict'
import test from 'node:test'
import { createNotificationLoader } from './notificationLoader.ts'

test('coalesces repeated loads while the first request is pending', async () => {
  let resolveRequest
  let calls = 0
  const loader = createNotificationLoader(
    () =>
      new Promise((resolve) => {
        calls += 1
        resolveRequest = resolve
      })
  )

  const first = loader.load()
  const second = loader.load(true)
  assert.equal(calls, 1)
  assert.equal(first, second)

  resolveRequest([{ id: 1 }])
  assert.deepEqual(await first, [{ id: 1 }])
})

test('serves cached data immediately and refreshes only when requested', async () => {
  let calls = 0
  const loader = createNotificationLoader(async () => [{ id: ++calls }])

  assert.deepEqual(await loader.load(), [{ id: 1 }])
  assert.deepEqual(loader.peek(), [{ id: 1 }])
  assert.deepEqual(await loader.load(), [{ id: 1 }])
  assert.equal(calls, 1)

  assert.deepEqual(await loader.load(true), [{ id: 2 }])
  assert.equal(calls, 2)
})

test('allows retry after a failed request and clears stale user data', async () => {
  let calls = 0
  const loader = createNotificationLoader(async () => {
    calls += 1
    if (calls === 1) throw new Error('temporary failure')
    return [{ id: 2 }]
  })

  await assert.rejects(loader.load(), /temporary failure/)
  assert.deepEqual(await loader.load(), [{ id: 2 }])
  loader.clear()
  assert.equal(loader.peek(), undefined)
})
