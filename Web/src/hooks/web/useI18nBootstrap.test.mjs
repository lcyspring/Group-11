import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./useI18n.ts', import.meta.url), 'utf8')
const plugin = readFileSync(new URL('../../plugins/vueI18n/index.ts', import.meta.url), 'utf8')

test('module-level translators use a synchronously-created i18n instance', () => {
  assert.doesNotMatch(source, /if\s*\(!i18n\)\s*\{\s*return\s+normalFn/)
  assert.match(source, /if\s*\(!i18n\)\s*return\s+resolvedKey/)
  assert.match(source, /const translated = i18n\.global\.t\(resolvedKey/)
  assert.match(plugin, /export const i18n = createI18n\(/)
  assert.match(plugin, /messages:\s*localeMessages/)
  assert.doesNotMatch(plugin, /await import\(/)
  assert.match(source, /resolveLocaleMessage\(localeMessages, locale, 'zh-CN'/)
})
