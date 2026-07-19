import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveLocaleMessage } from './messageResolver.ts'

const messages = {
  'zh-CN': {
    common: {
      router: { home: '首页' },
      operateSuccess: '{text}成功',
      listMessage: '{0} / {missing}'
    },
    workplace: { welcome: '你好' },
    analysis: { userAccessSource: '用户访问来源' },
    action: { more: '更多' }
  },
  en: { workplace: { welcome: 'Hello' } }
}

test('resolves the regression keys from raw locale objects', () => {
  assert.equal(resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'common.router.home'), '首页')
  assert.equal(resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'workplace.welcome'), '你好')
  assert.equal(
    resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'analysis.userAccessSource'),
    '用户访问来源'
  )
  assert.equal(resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'action.more'), '更多')
})

test('supports fallback locale and named interpolation', () => {
  assert.equal(resolveLocaleMessage(messages, 'ar', 'zh-CN', 'workplace.welcome'), '你好')
  assert.equal(
    resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'common.operateSuccess', [{ text: '保存' }]),
    '保存成功'
  )
  assert.equal(
    resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'common.listMessage', [['第一项']]),
    '第一项 / {missing}'
  )
  assert.equal(resolveLocaleMessage(messages, 'zh-CN', 'zh-CN', 'missing.key'), undefined)
})
