import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const productForm = await readFile(
  new URL('./components/BusinessProductForm.vue', import.meta.url),
  'utf8'
)
const businessForm = await readFile(new URL('./BusinessForm.vue', import.meta.url), 'utf8')

test('business product rows expose visible deletion only while editable', () => {
  assert.match(productForm, /v-if="!disabled"[^>]*:label="t\('common\.action'\)"/)
  assert.match(productForm, /handleDelete\(\$index\)/)
  assert.match(productForm, /t\('common\.delete'\)/)
  assert.match(productForm, /v-model="row\.businessPrice"/)
  assert.match(productForm, /v-model="row\.count"/)
  assert.match(productForm, /v-model="row\.taxRatePercent"/)
})

test('business totals tolerate an incomplete newly added product row', () => {
  assert.match(businessForm, /Number\(curr\.totalPrice \|\| 0\)/)
})
