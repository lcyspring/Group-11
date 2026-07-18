import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const vitePlugins = await readFile(new URL('./index.ts', import.meta.url), 'utf8')

test('production icon generation uses the CommonJS plugin and local collections', () => {
  assert.match(vitePlugins, /createRequire\(import\.meta\.url\)/)
  assert.match(vitePlugins, /nodeRequire\('vite-plugin-purge-icons'\)/)
  assert.match(vitePlugins, /PurgeIcons\(\{ iconSource: 'local' \}\)/)
  assert.doesNotMatch(vitePlugins, /remoteDataAPI/)
  assert.doesNotMatch(vitePlugins, /PurgeIcons\(\)/)
})
