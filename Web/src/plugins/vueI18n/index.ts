import type { App } from 'vue'
import { createI18n } from 'vue-i18n'
import { useLocaleStoreWithOut } from '@/store/modules/locale'
import { setHtmlPageLang } from './helper'
import zhCN from '../../locales/zh-CN/index'
import en from '../../locales/en/index'
import ar from '../../locales/ar/index'

export const localeMessages = {
  'zh-CN': zhCN,
  en,
  ar
}

const localeStore = useLocaleStoreWithOut()
const requestedLang = localeStore.getCurrentLocale.lang
const currentLang = requestedLang in localeMessages ? requestedLang : 'zh-CN'

setHtmlPageLang(currentLang)
localeStore.setCurrentLocale({ lang: currentLang })

// Create the complete instance during module evaluation. Shared route, chart,
// and form modules call t() at module scope, so an asynchronously-created
// instance permanently freezes raw keys into their exported constants.
export const i18n = createI18n({
  legacy: false,
  locale: currentLang,
  fallbackLocale: 'zh-CN',
  messages: localeMessages,
  sync: true,
  silentTranslationWarn: true,
  missingWarn: false,
  silentFallbackWarn: true
})

export const setupI18n = async (app: App<Element>) => {
  app.use(i18n)
}
