/** Build one explicit WebSocket URL from the configured API origin or current page origin. */
export function buildKefuWebSocketUrl(baseUrl, token, currentOrigin) {
  const normalizedToken = typeof token === 'string' ? token.trim() : ''
  if (!normalizedToken) return undefined

  const normalizedBaseUrl = typeof baseUrl === 'string' ? baseUrl.trim() : ''
  if (!normalizedBaseUrl && !currentOrigin) return undefined

  try {
    const base = new URL(normalizedBaseUrl || '/', currentOrigin)
    if (!['http:', 'https:', 'ws:', 'wss:'].includes(base.protocol)) return undefined
    const url = new URL('/infra/ws', base)
    if (url.protocol === 'http:') url.protocol = 'ws:'
    if (url.protocol === 'https:') url.protocol = 'wss:'
    url.searchParams.set('token', normalizedToken)
    return url.toString()
  } catch {
    return undefined
  }
}
