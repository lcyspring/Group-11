import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

import { buildKefuWebSocketUrl } from './webSocketUrl.mjs'

test('builds same-origin ws and wss URLs with an encoded refresh token', () => {
  assert.equal(
    buildKefuWebSocketUrl('', ' token+value ', 'http://crm.example:8081'),
    'ws://crm.example:8081/infra/ws?token=token%2Bvalue'
  )
  assert.equal(
    buildKefuWebSocketUrl('https://api.example/base', 'secure', 'http://ignored.example'),
    'wss://api.example/infra/ws?token=secure'
  )
  assert.equal(
    buildKefuWebSocketUrl('ws://socket.example', 'plain', undefined),
    'ws://socket.example/infra/ws?token=plain'
  )
  assert.equal(
    buildKefuWebSocketUrl('wss://socket.example', 'secure', undefined),
    'wss://socket.example/infra/ws?token=secure'
  )
})

test('rejects missing credentials and invalid origins', () => {
  assert.equal(buildKefuWebSocketUrl('', '', 'http://crm.example'), undefined)
  assert.equal(buildKefuWebSocketUrl(undefined, 123, 'http://crm.example'), undefined)
  assert.equal(buildKefuWebSocketUrl('', 'token', undefined), undefined)
  assert.equal(buildKefuWebSocketUrl(undefined, 'token', 'http://crm.example'), 'ws://crm.example/infra/ws?token=token')
  assert.equal(buildKefuWebSocketUrl('file:///tmp/index.html', 'token', undefined), undefined)
  assert.equal(buildKefuWebSocketUrl('not a url', 'token', undefined), undefined)
})

test('customer service runtime opens one socket and guards asynchronous DOM references', async () => {
  const page = await readFile(new URL('../../index.vue', import.meta.url), 'utf8')
  const messageList = await readFile(new URL('../KeFuMessageList.vue', import.meta.url), 'utf8')
  const memberInfo = await readFile(new URL('../member/MemberInfo.vue', import.meta.url), 'utf8')
  assert.match(page, /immediate:\s*false/)
  assert.match(page, /if \(server\.value\) open\(\)/)
  assert.doesNotMatch(messageList, /innerRef\.value!\.clientHeight/)
  assert.doesNotMatch(messageList, /wrap!\.(?:scrollHeight|clientHeight|scrollTop)/)
  assert.doesNotMatch(memberInfo, /wrap!\.(?:scrollHeight|clientHeight|scrollTop)/)
  assert.match(messageList, /requestVersion !== activeRequestVersion/)
  assert.match(messageList, /handleScroll\.cancel\(\)/)
  assert.match(memberInfo, /handleScroll\.cancel\(\)/)
})
