import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = new TextDecoder('utf-8', { fatal: true }).decode(
  readFileSync(new URL('./index.vue', import.meta.url))
)
const poolSource = new TextDecoder('utf-8', { fatal: true }).decode(
  readFileSync(new URL('../pool/index.vue', import.meta.url))
)

test('reloads the garbage page whenever its cached view becomes active', () => {
  assert.match(source, /const \{ push \} = useRouter\(\)/)
  assert.match(source, /onActivated\(getList\)/)
  assert.match(source, /watch\(customerGarbageRefreshRevision, getList\)/)
  assert.doesNotMatch(source, /onMounted\(getList\)/)
})

test('invalidates the cached garbage list immediately after a successful transition', () => {
  assert.match(
    poolSource,
    /await CustomerApi\.putCustomerGarbage\([\s\S]*?invalidateCustomerGarbageList\(\)[\s\S]*?await getList\(\)/
  )
  assert.match(source, /if \(listRequest\) return listRequest/)
})

test('keeps restore and permanent-delete actions visible on one stable row', () => {
  assert.match(source, /fixed="right" width="320"/)
  assert.match(source, /<div class="garbage-actions">[\s\S]*?restoreToPublic[\s\S]*?permanentDelete[\s\S]*?<\/div>/)
  assert.match(source, /\.garbage-actions \{[\s\S]*?display: inline-flex;[\s\S]*?white-space: nowrap;/)
  assert.match(source, /crm:customer-garbage:manage/)
  assert.match(source, /crm:customer-garbage:delete/)
})
