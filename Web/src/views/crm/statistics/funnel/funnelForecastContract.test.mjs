import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const page = await readFile(new URL('./index.vue', import.meta.url), 'utf8')
const funnel = await readFile(new URL('./components/FunnelBusiness.vue', import.meta.url), 'utf8')
const forecast = await readFile(new URL('./components/SalesForecast.vue', import.meta.url), 'utf8')
const api = await readFile(new URL('../../../../api/crm/statistics/funnel.ts', import.meta.url), 'utf8')
const locales = await Promise.all(['zh-CN', 'en', 'ar'].map((locale) =>
  readFile(new URL(`../../../../locales/${locale}/crm.ts`, import.meta.url), 'utf8')))

test('funnel separates stage progression from all terminal outcomes', () => {
  assert.match(funnel, /type: 'funnel'/)
  assert.match(funnel, /type: 'pie'/)
  assert.match(funnel, /getBusinessOutcomePage/)
  assert.match(api, /get-business-outcome-page/)
  assert.match(funnel, /CRM_BUSINESS_END_STATUS_TYPE/)
  assert.match(funnel, /statusGroupMissingStages/)
})

test('forecast explicitly compares forecast and actual won amounts by period', () => {
  assert.match(page, /forecastPeriod/)
  assert.match(page, /forecastGranularity/)
  assert.match(forecast, /forecastAmount/)
  assert.match(forecast, /actualAmount/)
  assert.match(forecast, /forecastMetricExplanation/)
  assert.doesNotMatch(forecast, /weightedAmount/)
  assert.doesNotMatch(api, /weightedAmount/)
})

test('all locales explain forecast and outcome metrics', () => {
  for (const locale of locales) {
    assert.match(locale, /forecastPeriod:/)
    assert.match(locale, /forecastMetricExplanation:/)
    assert.match(locale, /outcomeDistribution:/)
  }
})
