import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const view = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../../api/crm/marketing/index.ts', import.meta.url), 'utf8')
const locales = await Promise.all(
  ['zh-CN', 'en', 'ar'].map((locale) =>
    readFile(new URL(`../../../../locales/${locale}/crm.ts`, import.meta.url), 'utf8')
  )
)

test('competitor owner comes from the real user directory without a hardcoded identity', () => {
  assert.match(view, /UserApi\.getSimpleUserList\(\)/)
  assert.match(view, /v-model="formData\.ownerUserId"/)
  assert.doesNotMatch(view, /ownerUserId\s*:\s*1\b/)
})
test('competitor UI exposes maintainable create update delete and analysis fields', () => {
  assert.match(view, /openForm\('create'\)/)
  assert.match(view, /openForm\('update', row\)/)
  assert.match(view, /handleDelete\(row\.id\)/)
  for (const field of ['strengths', 'weaknesses', 'strategy', 'remark', 'status']) {
    assert.match(view, new RegExp(`formData\\.${field}`))
  }
  assert.match(view, /TableActions/)
  assert.match(view, /crm:competitor:update/)
  assert.match(view, /crm:competitor:delete/)
})

test('competitor website and API payload are explicitly validated and typed', () => {
  assert.match(view, /\^https\?:\\\/\\\//)
  assert.match(api, /interface MarketingCompetitorVO/)
  assert.match(api, /saveCompetitor = \(data: MarketingCompetitorVO\)/)
})

test('competitor additions have complete three-language labels', () => {
  for (const source of locales) {
    for (const key of ['updateCompetitor', 'strategy', 'remark', 'websiteInvalid']) {
      assert.match(source, new RegExp(`${key}:`))
    }
  }
})
