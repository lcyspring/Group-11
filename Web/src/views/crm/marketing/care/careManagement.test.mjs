import assert from 'node:assert/strict'
import test from 'node:test'
import { CareRecipientStatus, CareRuleType, isValidMonthDay, normalizeCarePlan, validateCarePlan } from './careManagement.mjs'

const base = () => ({
  code: 'CARE-1', name: 'Care', ruleType: CareRuleType.BIRTHDAY,
  channel: 1, smsTemplateCode: 'sms', enabled: false
})

test('care rules cover birthday, holiday, and post-deal follow-up', () => {
  assert.equal(validateCarePlan(base()), null)
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.HOLIDAY, eventMonthDay: '02-29' }), null)
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.HOLIDAY, eventMonthDay: '02-30' }), 'eventDate')
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.POST_DEAL_FOLLOW_UP, followUpDays: 7 }), null)
  assert.equal(validateCarePlan({ ...base(), ruleType: 9 }), 'rule')
})

test('holiday validation rejects impossible calendar dates', () => {
  assert.equal(isValidMonthDay('02-29'), true)
  assert.equal(isValidMonthDay('02-30'), false)
  assert.equal(isValidMonthDay('13-01'), false)
  assert.equal(isValidMonthDay('tomorrow'), false)
  assert.equal(isValidMonthDay(undefined), false)
})

test('channel templates are conditional and dual channel requires both', () => {
  assert.equal(validateCarePlan({ ...base(), smsTemplateCode: '' }), 'smsTemplate')
  assert.equal(validateCarePlan({ ...base(), channel: 2, smsTemplateCode: '', mailTemplateCode: 'mail' }), null)
  assert.equal(validateCarePlan({ ...base(), channel: 3, mailTemplateCode: '' }), 'mailTemplate')
  assert.equal(validateCarePlan({ ...base(), channel: 9 }), 'channel')
})

test('post-deal delay is bounded and normalized by rule', () => {
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.POST_DEAL_FOLLOW_UP, followUpDays: 0 }), 'followUpDays')
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.POST_DEAL_FOLLOW_UP, followUpDays: 1.5 }), 'followUpDays')
  assert.equal(validateCarePlan({ ...base(), ruleType: CareRuleType.POST_DEAL_FOLLOW_UP, followUpDays: 3651 }), 'followUpDays')
  const normalized = normalizeCarePlan({ ...base(), eventMonthDay: '01-01', followUpDays: 7 })
  assert.equal(normalized.eventMonthDay, undefined)
  assert.equal(normalized.followUpDays, undefined)
  const holiday = normalizeCarePlan({ ...base(), ruleType: CareRuleType.HOLIDAY, eventMonthDay: ' 01-01 ', channel: 2, smsTemplateCode: 'ignored', mailTemplateCode: ' mail ' })
  assert.equal(holiday.eventMonthDay, '01-01')
  assert.equal(holiday.smsTemplateCode, undefined)
  assert.equal(holiday.mailTemplateCode, 'mail')
  assert.equal(normalizeCarePlan({ ...base(), ruleType: CareRuleType.HOLIDAY }).eventMonthDay, undefined)
  const followUp = normalizeCarePlan({ ...base(), ruleType: CareRuleType.POST_DEAL_FOLLOW_UP, followUpDays: 7, channel: 3, mailTemplateCode: ' mail ' })
  assert.equal(followUp.followUpDays, 7)
  assert.equal(followUp.smsTemplateCode, 'sms')
  assert.equal(followUp.mailTemplateCode, 'mail')
  const missingTemplates = normalizeCarePlan({ ...base(), channel: 3, smsTemplateCode: undefined, mailTemplateCode: undefined })
  assert.equal(missingTemplates.smsTemplateCode, undefined)
  assert.equal(missingTemplates.mailTemplateCode, undefined)
})

test('recipient status values match the shared backend delivery contract', () => {
  assert.deepEqual(CareRecipientStatus, {
    PENDING: 10, SENDING: 15, SENT: 20, FAILED: 30, SUPPRESSED: 40, RECORDED: 50
  })
})
