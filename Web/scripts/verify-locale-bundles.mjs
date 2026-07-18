import assert from 'node:assert/strict'
import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'

const outputDir = resolve(process.argv[2] || 'dist-prod')
const html = readFileSync(resolve(outputDir, 'index.html'), 'utf8')
const mainMatch = html.match(/src="\/assets\/(index-[^"]+\.js)"/)
assert.ok(mainMatch, 'production index does not reference a hashed JavaScript entry')

const main = readFileSync(resolve(outputDir, 'assets', mainMatch[1]), 'utf8')
const localeChunks = new Map()
for (const locale of ['zh-CN', 'en', 'ar']) {
  const pattern = new RegExp(
    `"\\.\\.\\/\\.\\.\\/locales\\/${locale}\\/index\\.ts":\\(\\)=>[^,}]*import\\("\\.\\/(index-[A-Za-z0-9_-]+\\.js)"\\)`
  )
  const match = main.match(pattern)
  assert.ok(match, `production entry does not map the ${locale} locale bundle`)
  const file = resolve(outputDir, 'assets', match[1])
  const size = statSync(file).size
  assert.ok(size >= 10_000, `${locale} locale bundle is suspiciously small: ${size} bytes`)
  localeChunks.set(locale, match[1])
}

assert.equal(new Set(localeChunks.values()).size, localeChunks.size, 'locale bundles unexpectedly share one entry')
console.log(`Locale bundle integrity passed: ${[...localeChunks].map(([locale, file]) => `${locale}=${file}`).join(', ')}`)
