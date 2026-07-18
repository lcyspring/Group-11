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
const trackingController = await readFile(new URL(
  '../../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/app/marketing/AppCrmMarketingTrackingController.java',
  import.meta.url
), 'utf8')
const linkMigration = await readFile(new URL(
  '../../../../../../database/migrations/new-crm-marketing-link-click.sql', import.meta.url
), 'utf8')

test('broadcast maintenance exposes detail, draft deletion and recipient results', () => {
  assert.match(api, /broadcast\/get/)
  assert.match(api, /broadcast\/delete/)
  assert.match(api, /broadcast\/recipients/)
  assert.match(api, /broadcast\/delivery-summary/)
  assert.match(api, /broadcast\/sync-results/)
  assert.match(controller, /@GetMapping\("\/broadcast\/get"\)/)
  assert.match(controller, /@DeleteMapping\("\/broadcast\/delete"\)/)
  assert.match(page, /openRecipients/)
  assert.match(page, /syncDeliveryResults/)
  assert.match(page, /smsDeliveryRate/)
  assert.match(page, /emailOpenRate/)
})

test('delivery analytics distinguishes SMS delivery from email acceptance and opening', () => {
  assert.match(controller, /@GetMapping\("\/broadcast\/delivery-summary"\)/)
  assert.match(controller, /@PutMapping\("\/broadcast\/sync-results"\)/)
  assert.match(service, /SmsReceiveStatusEnum\.SUCCESS/)
  assert.match(service, /MailSendStatusEnum\.SUCCESS/)
  assert.match(service, /recordMailOpen/)
})

test('click tracking uses stored targets, opaque tokens and separate unique and total metrics', () => {
  assert.match(page, /formData\.links/)
  assert.match(page, /uniqueClickRate/)
  assert.match(page, /totalClickCount/)
  assert.match(api, /MarketingLinkVO/)
  assert.match(service, /recordLinkClick/)
  assert.match(service, /isAllowedTargetUrl/)
  assert.match(service, /params\.put\(link\.getCode\(\), buildClickUrl/)
  assert.match(trackingController, /@GetMapping\("\/click\/\{token\}"\)/)
  assert.match(trackingController, /location\(URI\.create\(target\)\)/)
  assert.doesNotMatch(trackingController, /RequestParam.*target/)
  assert.match(linkMigration, /crm_marketing_link_recipient/)
  assert.match(linkMigration, /first_clicked_at/)
  assert.match(linkMigration, /click_count/)
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

test('zero-sendable drafts remain inspectable instead of being rolled back', () => {
  assert.doesNotMatch(service, /if \(row\.getValidCount\(\) == 0\) throw exception\(MARKETING_RECIPIENT_NOT_FOUND\)/)
  assert.match(service, /MARKETING_RECIPIENT_NONE_SENDABLE/)
  assert.match(page, /saved\.validCount/)
  assert.match(page, /noSendableRecipients/)
  assert.match(page, /openRecipients\(saved\)/)
})

test('recipient results can repair consent and rebuild a draft', () => {
  assert.match(api, /refresh-recipients/)
  assert.match(controller, /@PutMapping\("\/broadcast\/refresh-recipients"\)/)
  assert.match(service, /refreshDraftRecipients/)
  assert.match(page, /setRecipientConsent/)
  assert.match(page, /saveConsent/)
  assert.match(page, /refreshBroadcastRecipients/)
  assert.match(page, /grantConsent/)
  assert.match(page, /optOut/)
})
