import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./Index.vue', import.meta.url), 'utf8')

test('home summary and notice controls navigate to working destinations', () => {
  assert.match(source, /@click="handleTodoClick"/)
  assert.match(source, /router\.push\('\/bpm\/task\/todo'\)/)
  assert.match(source, /@click="handleNoticeMoreClick"/)
  assert.match(source, /router\.hasRoute\('SystemNotice'\)/)
  assert.match(source, /name: canOpenNoticeManagement \? 'SystemNotice' : 'MyNotifyMessage'/)
  assert.doesNotMatch(source, /router\.push\('\/system\/notice'\)/)
})

test('home reloads real todo and notice data without trapping the page skeleton', () => {
  assert.match(source, /TaskApi\.getTaskTodoPage/)
  assert.match(source, /NoticeApi\.getNoticePage/)
  assert.match(source, /totalSate\.notice = page\.total/)
  assert.match(source, /Promise\.allSettled/)
  assert.match(source, /finally\s*\{\s*loading\.value = false/)
  assert.match(source, /onActivated/)
  assert.doesNotMatch(source, /access:\s*2340|project:\s*40/)
})
