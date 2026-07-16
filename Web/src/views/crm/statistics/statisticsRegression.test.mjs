import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readUtf8 = (relativePath) =>
  new TextDecoder('utf-8', { fatal: true }).decode(
    readFileSync(new URL(relativePath, import.meta.url))
  )

test('funnel tab refs remain executable UTF-8 declarations', () => {
  const source = readUtf8('./funnel/index.vue')
  const handleQueryIndex = source.indexOf('const handleQuery = async () =>')

  assert.ok(handleQueryIndex > 0)
  for (const name of [
    'funnelRef',
    'businessSummaryRef',
    'businessInversionRateSummaryRef',
    'salesForecastRef'
  ]) {
    const declaration = `const ${name} = ref(`
    const declarationIndex = source.indexOf(declaration)
    assert.ok(declarationIndex >= 0, `${name} must be declared`)
    assert.ok(declarationIndex < handleQueryIndex, `${name} must be declared before use`)
  }
})

test('business summary pagination overrides parent filters explicitly', () => {
  const source = readUtf8('./funnel/components/BusinessSummary.vue')

  assert.match(
    source,
    /getBusinessPageByDate\(\{\s*\.\.\.props\.queryParams,\s*pageNo: queryParams0\.pageNo,\s*pageSize: queryParams0\.pageSize\s*\}\)/
  )
})

test('performance year selector remains separate from timestamp range', () => {
  const source = readUtf8('./performance/index.vue')

  assert.match(source, /v-model="selectedYear"/)
  assert.match(source, /const selectedYear = ref\(String\(currentYear\)\)/)
  assert.match(source, /queryParams\.times\[0\] = formatDate\(beginOfDay\(new Date\(selectYear, 0, 1\)\)\)/)
  assert.match(source, /queryParams\.times\[1\] = formatDate\(endOfDay\(new Date\(selectYear, 11, 31\)\)\)/)
  assert.doesNotMatch(source, /v-model="queryParams\.times"/)
})

test('statistics loaders always release loading state after request failure', () => {
  for (const relativePath of [
    './customer/components/CustomerSummary.vue',
    './customer/components/CustomerConversionStat.vue',
    './funnel/components/BusinessSummary.vue'
  ]) {
    const source = readUtf8(relativePath)
    assert.match(
      source,
      /const loadData = async \(\) => \{[\s\S]*?loading\.value = true[\s\S]*?try \{[\s\S]*?await [\w()]+[\s\S]*?\} finally \{[\s\S]*?loading\.value = false[\s\S]*?\}/,
      relativePath
    )
  }
})
