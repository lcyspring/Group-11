import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./main.vue', import.meta.url), 'utf8')

test('MP account selector resolves module-local translations explicitly', () => {
  assert.match(source, /mp\.common\.selectAccount/)
  assert.match(source, /mp\.common\.noAccountConfig/)
  assert.doesNotMatch(source, /t\('common\.noAccountConfig'\)/)
})

test('missing account warning is deduplicated across selector remounts', () => {
  assert.match(source, /let missingAccountNotified = false/)
  assert.match(source, /if \(!missingAccountNotified\)/)
  assert.match(source, /missingAccountNotified = true/)
  assert.match(source, /missingAccountNotified = false/)
})

test('mounted request rejection is contained by the component', () => {
  assert.match(source, /try \{/)
  assert.match(source, /catch \{/)
  assert.match(source, /void handleQuery\(\)/)
})
