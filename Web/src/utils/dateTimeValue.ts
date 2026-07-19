import dayjs from 'dayjs'

export type DateTimeValue = string | number | Date | null | undefined

const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'

/**
 * Normalize API date/time values before they reach a view.
 *
 * Java services can serialize the same LocalDateTime field as an epoch value or
 * a formatted string depending on the active serializer. Keeping this at the
 * API boundary prevents raw timestamps from leaking into tables and forms.
 */
export const formatDateTimeValue = (value: DateTimeValue): string => {
  if (value === null || value === undefined || value === '') return ''
  const candidate = typeof value === 'string' && /^\d{13}$/.test(value) ? Number(value) : value
  const date = dayjs(candidate)
  return date.isValid() ? date.format(DATE_TIME_FORMAT) : ''
}
