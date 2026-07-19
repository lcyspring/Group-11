import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const component = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')

test('menu trigger is not rendered when conditional actions are empty', () => {
  assert.match(component, /mode === 'menu' && hasDefaultActions\(\)/)
  assert.match(component, /node\.type === Comment/)
  assert.match(component, /node\.type === Fragment/)
  assert.match(component, /slots\.default\?\.\(\)/)
})
