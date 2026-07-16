import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./main.vue', import.meta.url), 'utf8')

test('MP account selector resolves module-local translations explicitly', () => {
  assert.match(source, /mp\.common\.selectAccount/)
  assert.doesNotMatch(source, /t\('common\.noAccountConfig'\)/)
})

test('missing account stays on the selected submenu and offers explicit account configuration', () => {
  assert.match(source, /accountList\.value\.length === 0/)
  assert.match(source, /v-if="!loading && accountList\.length === 0"/)
  assert.match(source, /@click="openAccountManagement"/)
  assert.match(source, /const openAccountManagement = \(\) => push\(\{ name: 'MpAccount' \}\)/)
  assert.doesNotMatch(source, /delView/)
  assert.doesNotMatch(source, /currentRoute/)
  assert.doesNotMatch(source, /message\.(warning|error)/)
  assert.doesNotMatch(source, /useMessage\(\)/)
})

test('mounted request rejection is contained by the component', () => {
  assert.match(source, /try \{/)
  assert.match(source, /catch \{/)
  assert.match(source, /loadFailed\.value = true/)
  assert.match(source, /finally \{/)
  assert.match(source, /void handleQuery\(\)/)
})

test('selector honors v-model and reports account changes consistently', () => {
  assert.match(source, /modelValue\?: number/)
  assert.match(source, /item\.id === props\.modelValue/)
  assert.match(source, /emit\('update:modelValue', account\.id\)/)
  assert.match(source, /emit\('update:modelValue', found\.id\)/)
})
