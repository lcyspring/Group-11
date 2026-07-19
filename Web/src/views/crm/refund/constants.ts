export const toRefundEpochMillis = (value: string | number) => {
  const timestamp = Number(value)
  if (!Number.isFinite(timestamp) || timestamp <= 0) {
    throw new RangeError('refund time must be a positive epoch-millisecond value')
  }
  return Math.trunc(timestamp)
}
