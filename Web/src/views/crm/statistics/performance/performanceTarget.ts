const AMOUNT_PATTERN = /^\d{1,18}(?:\.\d{1,2})?$/
const COUNT_PATTERN = /^\d{1,18}$/

export const isValidTargetValue = (value: string, countTarget: boolean): boolean =>
  (countTarget ? COUNT_PATTERN : AMOUNT_PATTERN).test(value.trim())

const toUnits = (value: string, scale: number): bigint | null => {
  if (!isValidTargetValue(value, scale === 0)) return null
  const [integerPart, fractionPart = ''] = value.trim().split('.')
  const factor = 10n ** BigInt(scale)
  return BigInt(integerPart) * factor + BigInt(fractionPart.padEnd(scale, '0') || '0')
}

const fromUnits = (value: bigint, scale: number): string => {
  if (scale === 0) return value.toString()
  const factor = 10n ** BigInt(scale)
  const integerPart = value / factor
  const fractionPart = (value % factor).toString().padStart(scale, '0')
  return `${integerPart}.${fractionPart}`
}

/** 精确汇总目标值，避免金额通过 JavaScript Number 累计后丢失精度。 */
export const sumTargetValues = (values: string[], countTarget: boolean): string | null => {
  const scale = countTarget ? 0 : 2
  let total = 0n
  for (const value of values) {
    const units = toUnits(value, scale)
    if (units === null) return null
    total += units
  }
  return fromUnits(total, scale)
}

export const buildQuarterTargets = (
  monthlyTargets: string[],
  countTarget: boolean
): Array<string | null> =>
  Array.from({ length: 4 }, (_, quarter) => {
    const start = quarter * 3
    return sumTargetValues(monthlyTargets.slice(start, start + 3), countTarget)
  })
