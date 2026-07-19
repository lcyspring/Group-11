export const calculateWorkingDays = (startTime, endTime) => {
  if (!startTime || !endTime || Number(endTime) <= Number(startTime)) return 0
  const current = new Date(Number(startTime))
  const end = new Date(Number(endTime))
  current.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  let days = 0
  while (current <= end) {
    const weekday = current.getDay()
    if (weekday !== 0 && weekday !== 6) days += 1
    current.setDate(current.getDate() + 1)
  }
  return days
}
