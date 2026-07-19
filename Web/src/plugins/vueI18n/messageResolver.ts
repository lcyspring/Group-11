type MessageTree = Record<string, unknown>

const getMessage = (tree: unknown, key: string): unknown => {
  return key.split('.').reduce<unknown>((current, part) => {
    if (!current || typeof current !== 'object') return undefined
    return (current as MessageTree)[part]
  }, tree)
}

const interpolate = (message: string, parameters: unknown[]): string => {
  const list = parameters.find(Array.isArray)
  const named = parameters.find(
    (parameter) => parameter !== null && typeof parameter === 'object' && !Array.isArray(parameter)
  ) as MessageTree | undefined

  return message.replace(/\{([^{}]+)\}/g, (placeholder, name: string) => {
    if (named && named[name] !== undefined) return String(named[name])
    if (list && /^\d+$/.test(name) && list[Number(name)] !== undefined) return String(list[Number(name)])
    return placeholder
  })
}

/**
 * Resolve raw TypeScript locale objects when vue-i18n cannot compile a message.
 * This is deliberately a fallback: vue-i18n remains responsible for plural,
 * linked-message, and locale-aware formatting whenever it returns a value.
 */
export const resolveLocaleMessage = (
  messages: Record<string, unknown>,
  locale: string,
  fallbackLocale: string,
  key: string,
  parameters: unknown[] = []
): string | undefined => {
  const message = getMessage(messages[locale], key) ?? getMessage(messages[fallbackLocale], key)
  return typeof message === 'string' ? interpolate(message, parameters) : undefined
}
