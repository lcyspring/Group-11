import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const form = await readFile(new URL('./ProductCategoryForm.vue', import.meta.url), 'utf8')
const list = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../../api/crm/product/category/index.ts', import.meta.url), 'utf8')
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

test('product category CRUD keeps typed payloads and does not swallow request failures', () => {
  assert.match(api, /ProductCategoryCreateReqVO/)
  assert.match(api, /ProductCategoryUpdateReqVO/)
  assert.match(form, /ref<FormInstance>/)
  assert.match(form, /createProductCategory\(\{ name, parentId \}\)/)
  assert.match(form, /updateProductCategory\(\{ id, name, parentId \}\)/)
  assert.doesNotMatch(form, /as unknown as|ref<any/)
  assert.doesNotMatch(list, /catch\s*\{\s*\}/)
})
