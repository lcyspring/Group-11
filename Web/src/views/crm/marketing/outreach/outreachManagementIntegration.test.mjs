import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const page = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../../api/crm/marketing/index.ts', import.meta.url), 'utf8')
const controller = await readFile(new URL(
  '../../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/marketing/CrmMarketingOutreachController.java',
  import.meta.url
), 'utf8')
const service = await readFile(new URL(
  '../../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/marketing/CrmMarketingOutreachService.java',
  import.meta.url
), 'utf8')

test('broadcast maintenance exposes detail, draft deletion and recipient results', () => {
  assert.match(api, /broadcast\/get/)
  assert.match(api, /broadcast\/delete/)
  assert.match(api, /broadcast\/recipients/)
  assert.match(controller, /@GetMapping\("\/broadcast\/get"\)/)
  assert.match(controller, /@DeleteMapping\("\/broadcast\/delete"\)/)
  assert.match(page, /openRecipients/)
})

test('page uses actual customer and contact selectors instead of comma separated IDs', () => {
  assert.match(page, /MarketingApi\.getBroadcastTargetOptions/)
  assert.match(api, /broadcast\/target-options/)
  assert.match(controller, /@GetMapping\("\/broadcast\/target-options"\)/)
  assert.match(page, /v-model="formData\.customerIds"/)
  assert.match(page, /v-model="formData\.contactIds"/)
  assert.doesNotMatch(page, /customerIdsText/)
  assert.match(page, /Promise\.allSettled/)
})

test('review, sending and retry commands are available and guarded by backend ownership', () => {
  assert.match(page, /approveBroadcast/)
  assert.match(page, /rejectBroadcast/)
  assert.match(page, /retryBroadcast/)
  assert.match(service, /requireCreatorOrAdmin/)
  assert.match(service, /reviewIfPending/)
  assert.match(service, /MARKETING_REVIEW_COMMENT_REQUIRED/)
})
