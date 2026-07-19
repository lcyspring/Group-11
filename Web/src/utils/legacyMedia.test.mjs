import assert from 'node:assert/strict'
import test from 'node:test'
import {
  normalizeLegacyMediaPayload,
  normalizeLegacyMediaUrl,
  parseLegacyMediaOrigins
} from './legacyMedia.ts'

const origins = parseLegacyMediaOrigins(
  ' http://test.yudao.iocoder.cn,https://retired-media.example.test '
)

test('blocks only explicitly configured retired media origins', () => {
  assert.deepEqual(parseLegacyMediaOrigins(' ,not-a-valid-origin'), [])
  assert.deepEqual(parseLegacyMediaOrigins(), [])
  assert.equal(normalizeLegacyMediaUrl(null, origins), '')
  assert.equal(
    normalizeLegacyMediaUrl(
      'http://test.yudao.iocoder.cn/user/avatar/20251220/blob_1766215463801.jpg',
      origins
    ),
    ''
  )
  assert.equal(
    normalizeLegacyMediaUrl('https://retired-media.example.test/avatar.png', origins),
    ''
  )
  assert.equal(
    normalizeLegacyMediaUrl('https://active.example.test/avatar.png', origins),
    'https://active.example.test/avatar.png'
  )
  assert.equal(normalizeLegacyMediaUrl('/admin-api/infra/file/4/get/avatar.png', origins),
    '/admin-api/infra/file/4/get/avatar.png')
})

test('normalizes retired media throughout cached and API payloads', () => {
  const payload = {
    user: {
      avatar: 'http://test.yudao.iocoder.cn/user/avatar/20251220/blob_1766215463801.jpg'
    },
    members: [
      { avatar: 'https://active.example.test/avatar.png' },
      { avatar: null }
    ]
  }
  assert.equal(normalizeLegacyMediaPayload(payload, origins), payload)
  assert.equal(payload.user.avatar, '')
  assert.equal(payload.members[0].avatar, 'https://active.example.test/avatar.png')
  assert.equal(payload.members[1].avatar, null)
})

test('handles repeated and circular response references', () => {
  const shared = {
    avatar: 'http://test.yudao.iocoder.cn/avatar.png'
  }
  const payload = { first: shared, second: shared }
  payload.self = payload
  normalizeLegacyMediaPayload(payload, origins)
  assert.equal(payload.first.avatar, '')
  assert.equal(payload.second, shared)
  assert.equal(payload.self, payload)
})
