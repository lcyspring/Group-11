/**
 * Accept only an explicit source component path for BPM custom detail views.
 * Browser routes and extension guessing are deliberately rejected: silently
 * resolving them previously embedded list pages in approval details.
 */
export function resolveBusinessFormComponentPath(value) {
  if (typeof value !== 'string') return undefined
  const path = value.trim().split(/[?#]/, 1)[0]
  if (!path.startsWith('/') || path.startsWith('//')) return undefined
  return path.endsWith('.vue') || path.endsWith('.tsx') ? path : undefined
}

