export const calculateTripDays = (startTime, endTime) => {
  const start = Number(startTime)
  const end = Number(endTime)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return 0
  return Math.ceil(((end - start) / 86400000) * 100) / 100
}

export const isFutureTrip = (startTime, now = Date.now()) => Number(startTime) > now
