import { formatDateTimeValue, type DateTimeValue } from '../../../utils/dateTimeValue'

export type EventTimeValue = DateTimeValue
export const formatEventTime = formatDateTimeValue

export const normalizeEventTimes = <T extends { startTime: EventTimeValue; endTime: EventTimeValue }>(
  event: T
): Omit<T, 'startTime' | 'endTime'> & { startTime: string; endTime: string } => ({
  ...event,
  startTime: formatEventTime(event.startTime),
  endTime: formatEventTime(event.endTime)
})
