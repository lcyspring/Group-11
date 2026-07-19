export const CampaignStatus = Object.freeze({
  DRAFT: 10,
  ACTIVE: 20,
  LOCKED: 30,
  TERMINATED: 40,
  COMPLETED: 50
})

export const isEndAfterStart = (startTime, endTime) =>
  Boolean(startTime && endTime && Number(endTime) > Number(startTime))

export const campaignActionVisibility = (status) => ({
  edit: status === CampaignStatus.DRAFT,
  start: status === CampaignStatus.DRAFT,
  delete: status === CampaignStatus.DRAFT,
  lock: status === CampaignStatus.ACTIVE,
  terminate: status === CampaignStatus.ACTIVE || status === CampaignStatus.LOCKED,
  complete: status === CampaignStatus.ACTIVE || status === CampaignStatus.LOCKED
})
