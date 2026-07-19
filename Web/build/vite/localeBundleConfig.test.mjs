import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('./index.ts', import.meta.url), 'utf8')

test('Vue i18n compilation never consumes TypeScript locale aggregators', () => {
  assert.match(source, /src\/locales\/\*\*\/\*\.\{json,json5,yaml,yml\}/)
  assert.doesNotMatch(source, /include:\s*\[pathResolve\('src\/locales\/\*\*'\)\]/)
  assert.match(
    source,
    /runtimeOnly:\s*false/,
    'raw TypeScript locale messages require the production message compiler'
  )
})
