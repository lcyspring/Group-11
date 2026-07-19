import assert from 'node:assert/strict'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { resolve } from 'node:path'

const outputDir = resolve(process.argv[2] || 'dist-prod')
const vitePlugin = readFileSync(resolve('build/vite/index.ts'), 'utf8')
assert.match(
  vitePlugin,
  /runtimeOnly:\s*false/,
  'TypeScript locale objects contain raw strings and require the vue-i18n message compiler'
)
const plugin = readFileSync(resolve('src/plugins/vueI18n/index.ts'), 'utf8')
for (const locale of ['zh-CN', 'en', 'ar']) {
  assert.match(plugin, new RegExp(`locales/${locale}/index`), `i18n must statically import ${locale}`)
  const sourceFile = resolve('src', 'locales', locale, 'index.ts')
  assert.ok(statSync(sourceFile).size >= 1_000, `${locale} locale source is suspiciously small`)
}
assert.match(plugin, /export const i18n = createI18n\(/, 'i18n must be created during module evaluation')
assert.match(plugin, /fallbackLocale:\s*'zh-CN'/, 'Chinese must remain the explicit fallback locale')
assert.match(plugin, /messages:\s*localeMessages/, 'all locale messages must be registered synchronously')
assert.doesNotMatch(plugin, /await import\(/, 'locale bootstrap must not defer translation availability')
const i18nHook = readFileSync(resolve('src/hooks/web/useI18n.ts'), 'utf8')
assert.doesNotMatch(
  i18nHook,
  /if\s*\(!i18n\)\s*\{\s*return\s+normalFn/,
  'useI18n must not permanently capture the pre-bootstrap key-only translator'
)
assert.match(
  i18nHook,
  /const translated = i18n\.global\.t\(resolvedKey/,
  'useI18n must resolve the active locale translator at invocation time'
)
assert.match(
  i18nHook,
  /resolveLocaleMessage\(localeMessages, locale, 'zh-CN'/,
  'useI18n must fall back to raw TypeScript locale objects when compilation is unavailable'
)
const html = readFileSync(resolve(outputDir, 'index.html'), 'utf8')
const mainMatch = html.match(/src="\/assets\/(index-[^"]+\.js)"/)
assert.ok(mainMatch, 'production index does not reference a hashed JavaScript entry')

const assetsDir = resolve(outputDir, 'assets')
const assetNames = readdirSync(assetsDir)
const javascript = assetNames
  .filter((name) => name.endsWith('.js'))
  .map((name) => readFileSync(resolve(assetsDir, name), 'utf8'))
  .join('\n')
const decodedJavascript = javascript
  .replace(/\\u\{([0-9a-f]+)\}/gi, (_, codePoint) => String.fromCodePoint(Number.parseInt(codePoint, 16)))
  .replace(/\\u([0-9a-f]{4})/gi, (_, codePoint) => String.fromCharCode(Number.parseInt(codePoint, 16)))
for (const translation of ['祝你开心每一天!', 'Wish you happy every day!', 'أتمنى لك يوماً سعيداً!']) {
  assert.ok(decodedJavascript.includes(translation), `production output is missing locale sentinel: ${translation}`)
}
console.log(`Locale bundle integrity passed: entry=${mainMatch[1]}, locales=zh-CN,en,ar`)
