import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (relativePath) =>
  new TextDecoder('utf-8', { fatal: true }).decode(
    readFileSync(new URL(relativePath, import.meta.url))
  )

const actionBar = read('../../components/TableActions/index.vue')
const financeLists = [
  './receivable/index.vue',
  './reimbursement/index.vue',
  './refund/index.vue',
  './invoice/index.vue'
].map(read)
const financeForms = [
  './receivable/ReceivableForm.vue',
  './receivable/plan/ReceivablePlanForm.vue',
  './reimbursement/ReimbursementForm.vue',
  './reimbursement/ExpenseCategoryDialog.vue',
  './refund/RefundForm.vue',
  './invoice/InvoiceForm.vue',
  './invoice/InvoiceIssueDialog.vue',
  './invoice/InvoiceRedFlushDialog.vue',
  './invoice/InvoiceVoidDialog.vue'
].map(read)

test('finance action columns use one non-shrinking action bar', () => {
  assert.match(actionBar, /display: inline-flex/)
  assert.match(actionBar, /min-width: max-content/)
  assert.match(actionBar, /white-space: nowrap/)
  assert.match(actionBar, /flex: 0 0 auto/)
  for (const source of financeLists) {
    assert.match(
      source,
      /fixed="right"[^>]*width="(?:140|340)"|width="(?:140|340)"[^>]*fixed="right"/
    )
    assert.match(source, /<TableActions(?: mode="menu")?>[\s\S]*?<\/TableActions>/)
  }
})

test('shared action component provides a compact popup mode for crowded tables', () => {
  assert.match(actionBar, /mode\?: 'inline' \| 'menu'/)
  assert.match(actionBar, /v-if="mode === 'menu'"/)
  assert.match(actionBar, /<el-popover[\s\S]*?placement="bottom-end"[\s\S]*?trigger="click"/)
  assert.match(actionBar, /class="table-actions__menu" @click="menuVisible = false"/)
})

test('finance dialog actions use the global localized confirm and cancel keys', () => {
  for (const source of financeForms) {
    assert.doesNotMatch(source, /t\('dialog\.(?:confirm|cancel)'\)/)
    assert.match(source, /t\('common\.(?:confirm|cancel)'\)/)
  }
})
