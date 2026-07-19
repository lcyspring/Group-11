import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const vitePlugins = await readFile(new URL('./index.ts', import.meta.url), 'utf8')
const iconComponent = await readFile(new URL('../../src/components/Icon/src/Icon.vue', import.meta.url), 'utf8')

test('Vite plugins contain no legacy CommonJS icon generator', () => {
  assert.doesNotMatch(vitePlugins, /createRequire\(import\.meta\.url\)/)
  assert.doesNotMatch(vitePlugins, /vite-plugin-purge-icons/)
  assert.doesNotMatch(vitePlugins, /PurgeIcons/)
})

test('Iconify runtime uses a checked local snapshot without remote fallback', () => {
  assert.match(iconComponent, /getIconData, iconToSVG, replaceIDs/)
  assert.match(iconComponent, /offline-icon-collections\.generated\.json/)
  assert.doesNotMatch(iconComponent, /@iconify\/iconify/)
  assert.doesNotMatch(iconComponent, /Iconify\./)
  assert.doesNotMatch(iconComponent, /dataset\.icon|data-icon=/)
  assert.match(iconComponent, /immediate: true/)
})
