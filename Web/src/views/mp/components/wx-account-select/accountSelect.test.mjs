import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./main.vue', import.meta.url), 'utf8')

test('MP account selector resolves module-local translations explicitly', () => {
  assert.match(source, /mp\.common\.selectAccount/)
  assert.doesNotMatch(source, /t\('common\.noAccountConfig'\)/)
})

test('missing account redirects to account maintenance without repeated toast notifications', () => {
  assert.match(source, /accountList\.value\.length === 0/)
  assert.match(source, /push\(\{ name: 'MpAccount' \}\)/)
  assert.doesNotMatch(source, /message\.(warning|error)/)
  assert.doesNotMatch(source, /useMessage\(\)/)
})

test('mounted request rejection is contained by the component', () => {
  assert.match(source, /try \{/)
  assert.match(source, /catch \{/)
  assert.match(source, /void handleQuery\(\)/)
})
