export const parseLegacyMediaOrigins = (value?: string): string[] =>
  (value || '')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean)
    .flatMap((origin) => {
      try {
        return [new URL(origin).origin]
      } catch {
        return []
      }
    })

export const normalizeLegacyMediaUrl = (
  value: string | null | undefined,
  origins: readonly string[]
): string => {
  if (!value) return ''
  try {
    return origins.includes(new URL(value).origin) ? '' : value
  } catch {
    return value
  }
}

export const normalizeLegacyMediaPayload = <T>(
  value: T,
  origins: readonly string[],
  visited = new WeakSet<object>()
): T => {
  if (typeof value === 'string') {
    return normalizeLegacyMediaUrl(value, origins) as T
  }
  if (value === null || typeof value !== 'object' || visited.has(value)) return value

  visited.add(value)
  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      value[index] = normalizeLegacyMediaPayload(item, origins, visited)
    })
    return value
  }
  Object.keys(value).forEach((key) => {
    const record = value as Record<string, unknown>
    record[key] = normalizeLegacyMediaPayload(record[key], origins, visited)
  })
  return value
}
