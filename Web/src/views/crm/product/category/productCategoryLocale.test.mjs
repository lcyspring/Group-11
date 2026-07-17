import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const form = await readFile(new URL('./ProductCategoryForm.vue', import.meta.url), 'utf8')
const localeUrls = [
  new URL('../../../../locales/zh-CN/crm.ts', import.meta.url),
  new URL('../../../../locales/en/crm.ts', import.meta.url),
  new URL('../../../../locales/ar/crm.ts', import.meta.url)
]
const locales = await Promise.all(localeUrls.map((url) => readFile(url, 'utf8')))

test('product category form uses its crm.product namespace exactly once', () => {
  assert.doesNotMatch(form, /t\('product\.category\./)
  for (const key of ['parentId', 'parentIdPlaceholder', 'topCategory', 'namePlaceholder', 'nameRequired', 'parentIdRequired']) {
    assert.match(form, new RegExp(`t\\('category\\.${key}'\\)`))
  }
})

test('create and edit titles are explicit in every supported locale', () => {
  assert.match(form, /category\.createTitle/)
  assert.match(form, /category\.updateTitle/)
  for (const locale of locales) {
    assert.match(locale, /createTitle:/)
    assert.match(locale, /updateTitle:/)
  }
})
