import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./index.vue', import.meta.url), 'utf8')

test('public clue pool refreshes whenever its cached route becomes active', () => {
  assert.match(source, /onActivated\(getList\)/)
  assert.doesNotMatch(source, /onMounted\(getList\)/)
})
