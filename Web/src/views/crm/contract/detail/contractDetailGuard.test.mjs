import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const detailSource = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const lifecycleSource = await readFile(
  new URL('./ContractLifecyclePanel.vue', import.meta.url),
  'utf8'
)

test('contract detail does not mount id-dependent tabs before the contract is loaded', () => {
  assert.match(detailSource, /<el-col v-if="contract\.id">/)
  assert.match(detailSource, /Number\.isSafeInteger\(parsedId\)/)
  assert.match(detailSource, /parsedId <= 0/)
})
test('contract lifecycle refuses requests without a valid contract id', () => {
  assert.match(lifecycleSource, /const contractId = props\.contract\.id\s+if \(!contractId\) return/)
  assert.match(lifecycleSource, /getContractLifecycle\(contractId\)/)
  assert.match(lifecycleSource, /getContractAmendmentList\(contractId\)/)
  assert.match(lifecycleSource, /\(contractId\) => \{\s+if \(contractId\) load\(\)/)
})
