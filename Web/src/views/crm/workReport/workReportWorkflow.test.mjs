import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const view = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../../../api/crm/workReport/index.ts', import.meta.url), 'utf8')
const migration = readFileSync(new URL('../../../../../database/migrations/new-oa-work-report.sql', import.meta.url), 'utf8')
const response = readFileSync(new URL('../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/workreport/vo/CrmWorkReportRespVO.java', import.meta.url), 'utf8')

test('work reports support daily weekly monthly drafts and immutable submission', () => {
  assert.match(view, /daily.*weekly.*monthly/s)
  assert.match(view, /row\.status === 0/)
  assert.match(view, /submitWorkReport/)
  assert.match(api, /\/crm\/work-report\/submit/)
  assert.match(migration, /UNIQUE KEY uk_crm_work_report_period/)
})

test('authors and recipients have explicit list scopes and usable actions', () => {
  assert.match(view, /received: scope\.value === 'received'/)
  assert.match(view, /receiverUserIds/)
  assert.match(view, /TableActions mode="menu"/)
  assert.match(view, /await load\(\)/)
  assert.doesNotMatch(view, /t\('instance\.restart'\)/)
})

test('work report fields and localizations cover the full reporting contract', () => {
  for (const field of ['completedContent', 'pendingContent', 'nextPlan', 'issues', 'attachmentUrls']) {
    assert.match(view, new RegExp(field))
  }
  assert.match(view, /UserApi\.getSimpleUserList\(\)/)
  assert.match(view, /t\('workReport\.create'\)/)
  assert.doesNotMatch(view, /t\('common\.create'\)/)
  assert.equal([...response.matchAll(/@JsonFormat\(pattern = "yyyy-MM-dd"\)/g)].length, 3)
})
