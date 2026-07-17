import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const productForm = await readFile(
  new URL('./components/ContractProductForm.vue', import.meta.url),
  'utf8'
)
const contractForm = await readFile(new URL('./ContractForm.vue', import.meta.url), 'utf8')

test('editable contract product rows expose visible removal and editable commercial fields', () => {
  assert.match(productForm, /v-if="!disabled"[^>]*:label="t\('common\.action'\)"/)
  assert.match(productForm, /handleDelete\(\$index\)/)
  assert.match(productForm, /t\('common\.delete'\)/)
  assert.match(productForm, /v-model="row\.contractPrice"/)
  assert.match(productForm, /v-model="row\.count"/)
  assert.match(productForm, /formData\.value\.splice\(index, 1\)/)
})

test('contract totals remain numeric while a row is being added or edited', () => {
  assert.match(contractForm, /Number\(curr\.totalPrice \|\| 0\)/)
})
