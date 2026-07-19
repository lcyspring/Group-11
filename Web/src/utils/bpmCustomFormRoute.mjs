/**
 * Validate an explicitly configured browser route without rewriting it.
 * Invalid historical values must be fixed in model data, not guessed here.
 */
export const resolveCustomFormRoutePath = (rawPath) => {
  const value = rawPath?.trim()
  if (!value || !value.startsWith('/') || value.startsWith('//')) {
    return undefined
  }
  return value
}

/** Reject the catch-all 404 route even though vue-router reports it as matched. */
export const isResolvedBusinessRoute = (route) => {
  if (!route?.matched) return false
  for (const record of route.matched) {
    if (!record.path?.includes(':pathMatch')) return true
  }
  return false
}
