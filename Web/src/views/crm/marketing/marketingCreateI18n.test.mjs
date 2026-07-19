import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { extname } from 'node:path'

const pages = ['outreach/index.vue', 'care/index.vue', 'campaign/index.vue', 'competitor/index.vue']

for (const page of pages) {
  const source = readFileSync(new URL(page, import.meta.url), 'utf8')
  assert.doesNotMatch(source, /t\(['"]common\.create['"]\)/, `${page} must not use missing common.create`)
  assert.match(source, /t\(['"]action\.create['"]\)/, `${page} must use the shared create action`)
}

for (const locale of ['zh-CN', 'en', 'ar']) {
  const action = readFileSync(new URL(`../../../locales/${locale}/action.ts`, import.meta.url), 'utf8')
  assert.match(action, /create\s*:/, `${locale} must define action.create`)
}

const walkVueFiles = (directory) =>
  readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const target = new URL(`${entry.name}${entry.isDirectory() ? '/' : ''}`, directory)
    return entry.isDirectory() ? walkVueFiles(target) : extname(entry.name) === '.vue' ? [target] : []
  })

for (const view of walkVueFiles(new URL('../../../views/', import.meta.url))) {
  const source = readFileSync(view, 'utf8')
  assert.doesNotMatch(source, /t\(['"]common\.create['"]\)/, `${view.pathname} uses missing common.create`)
}

console.log('CRM marketing create i18n checks passed: pages=4 locales=3 global-missing-key=0')
