import { i18n, localeMessages } from '@/plugins/vueI18n'
import { resolveLocaleMessage } from '@/plugins/vueI18n/messageResolver'

type I18nGlobalTranslation = {
  (key: string): string
  (key: string, locale: string): string
  (key: string, locale: string, list: unknown[]): string
  (key: string, locale: string, named: Record<string, unknown>): string
  (key: string, list: unknown[]): string
  (key: string, named: Record<string, unknown>): string
}

type I18nTranslationRestParameters = [string, any]

// 顶级命名空间列表，这些命名空间不应该被添加前缀
const topLevelNamespaces = new Set([
  'common', 'action', 'table', 'dialog', 'form', 'error', 'login',
  'permission', 'setting', 'profile', 'lock', 'captcha', 'router',
  'analysis', 'workplace', 'watermark', 'sys', 'cropper', 'size',
  'system', 'mall', 'crm', 'erp', 'iot', 'bpm', 'ai', 'pay', 'member', 'mp', 'infra'
])

const getKey = (namespace: string | undefined, key: string) => {
  if (!namespace) {
    return key
  }
  if (key.startsWith(namespace)) {
    return key
  }
  // 检查 key 的第一部分是否是顶级命名空间
  const firstPart = key.split('.')[0]
  if (topLevelNamespaces.has(firstPart)) {
    return key
  }
  return `${namespace}.${key}`
}

export const useI18n = (
  namespace?: string
): {
  t: I18nGlobalTranslation
} => {
  const tFn: I18nGlobalTranslation = (key: string, ...arg: any[]) => {
    if (!key) return ''
    if (!key.includes('.') && !namespace) return key
    const resolvedKey = getKey(namespace, key)

    if (!i18n) return resolvedKey
    //@ts-ignore
    const translated = i18n.global.t(resolvedKey, ...(arg as I18nTranslationRestParameters))
    if (translated !== resolvedKey) return translated

    const locale =
      typeof i18n.global.locale === 'string' ? i18n.global.locale : i18n.global.locale.value
    return resolveLocaleMessage(localeMessages, locale, 'zh-CN', resolvedKey, arg) ?? translated
  }
  return {
    t: tFn
  }
}

export const t = (key: string) => key
