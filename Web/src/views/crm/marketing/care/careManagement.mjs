export const CareRuleType = Object.freeze({
  BIRTHDAY: 1,
  HOLIDAY: 2,
  POST_DEAL_FOLLOW_UP: 3
})

export const CareRecipientStatus = Object.freeze({
  PENDING: 10,
  SENDING: 15,
  SENT: 20,
  FAILED: 30,
  SUPPRESSED: 40,
  RECORDED: 50
})

export const isValidMonthDay = (value) => {
  if (!/^\d{2}-\d{2}$/.test(value || '')) return false
  const [month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(2000, month - 1, day))
  return date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

export const validateCarePlan = (plan) => {
  if (!Object.values(CareRuleType).includes(plan.ruleType)) return 'rule'
  if (![1, 2, 3].includes(plan.channel)) return 'channel'
  if ((plan.channel === 1 || plan.channel === 3) && !plan.smsTemplateCode?.trim()) return 'smsTemplate'
  if ((plan.channel === 2 || plan.channel === 3) && !plan.mailTemplateCode?.trim()) return 'mailTemplate'
  if (plan.ruleType === CareRuleType.HOLIDAY && !isValidMonthDay(plan.eventMonthDay)) return 'eventDate'
  if (plan.ruleType === CareRuleType.POST_DEAL_FOLLOW_UP &&
      (!Number.isInteger(plan.followUpDays) || plan.followUpDays < 1 || plan.followUpDays > 3650)) return 'followUpDays'
  return null
}

export const normalizeCarePlan = (plan) => ({
  ...plan,
  eventMonthDay: plan.ruleType === CareRuleType.HOLIDAY ? plan.eventMonthDay?.trim() : undefined,
  followUpDays: plan.ruleType === CareRuleType.POST_DEAL_FOLLOW_UP ? plan.followUpDays : undefined,
  smsTemplateCode: plan.channel === 2 ? undefined : plan.smsTemplateCode?.trim(),
  mailTemplateCode: plan.channel === 1 ? undefined : plan.mailTemplateCode?.trim()
})
