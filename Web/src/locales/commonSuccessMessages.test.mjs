import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readLocale = (locale) =>
  readFileSync(new URL(`./${locale}/common.ts`, import.meta.url), 'utf8')

test('common save success message exists in every shipped locale', () => {
  const expected = {
    'zh-CN': '保存成功',
    en: 'Saved successfully',
    ar: 'تم الحفظ بنجاح'
  }
  for (const [locale, message] of Object.entries(expected)) {
    assert.match(readLocale(locale), new RegExp(`saveSuccess: '${message}'`))
  }
})

test('pages using the shared save notification resolve through common locale', () => {
  for (const page of [
    '../views/bpm/oa/document/index.vue',
    '../views/bpm/oa/work-request/index.vue',
    '../views/bpm/oa/task/index.vue',
    '../views/crm/workReport/index.vue'
  ]) {
    assert.match(readFileSync(new URL(page, import.meta.url), 'utf8'), /t\('common\.saveSuccess'\)/)
  }
})
