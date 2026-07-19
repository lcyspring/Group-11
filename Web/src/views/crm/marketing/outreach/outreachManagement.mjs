export const BroadcastStatus = Object.freeze({
  DRAFT: 10,
  PENDING_REVIEW: 20,
  REJECTED: 30,
  READY: 40,
  SENDING: 50,
  SENT: 60,
  PARTIAL_FAILED: 70,
  CANCELLED: 80
})

export const RecipientStatus = Object.freeze({
  PENDING: 10,
  SENDING: 15,
  SENT: 20,
  FAILED: 30,
  SUPPRESSED: 40,
  RECORDED: 50
})

export const DeliveryStatus = Object.freeze({
  UNKNOWN: 0,
  PROVIDER_PENDING: 10,
  DELIVERED: 20,
  FAILED: 30,
  ACCEPTED: 40
})

export const formatMetricRate = (value) => `${Number(value ?? 0).toFixed(2)}%`

export const broadcastActionVisibility = (status) => ({
  edit: status === BroadcastStatus.DRAFT || status === BroadcastStatus.REJECTED,
  delete: status === BroadcastStatus.DRAFT,
  submit: status === BroadcastStatus.DRAFT,
  review: status === BroadcastStatus.PENDING_REVIEW,
  send: status === BroadcastStatus.READY,
  retry: status === BroadcastStatus.PARTIAL_FAILED,
  recipients: true
})

export const channelNeedsSms = (channel) => channel === 1 || channel === 3
export const channelNeedsEmail = (channel) => channel === 2 || channel === 3

export const isValidTemplateParams = (raw) => {
  if (!raw || !raw.trim()) return true
  try {
    const parsed = JSON.parse(raw)
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)
  } catch {
    return false
  }
}

export const hasTargets = (customerIds, contactIds) =>
  (customerIds?.length ?? 0) + (contactIds?.length ?? 0) > 0
