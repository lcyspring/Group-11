import { readFileSync, readdirSync, statSync } from 'node:fs'
import { relative, resolve } from 'node:path'

const root = resolve(process.cwd(), 'src/views')
const files = []

const walk = (directory) => {
  for (const name of readdirSync(directory)) {
    const path = resolve(directory, name)
    if (statSync(path).isDirectory()) walk(path)
    else if (name.endsWith('.vue')) files.push(path)
  }
}

const numberAttribute = (source, name) => {
  const match = source.match(new RegExp(`(?:^|\\s)(?:${name})=["'](\\d+)["']`))
  return match ? Number(match[1]) : undefined
}

const isActionColumn = (openingTag) =>
  /(?:operation|action)|label=["'](?:操作|Operation)["']|fixed=["']right["']/i.test(openingTag)

walk(root)

const findings = []
for (const file of files) {
  const source = readFileSync(file, 'utf8')
  const openingPattern = /<el-table-column\b[^>]*>/g
  for (const opening of source.matchAll(openingPattern)) {
    const openingTag = opening[0]
    if (/\/\s*>$/.test(openingTag)) continue
    const contentStart = opening.index + openingTag.length
    const closingStart = source.indexOf('</el-table-column>', contentStart)
    const nestedOpening = source.indexOf('<el-table-column', contentStart)
    if (closingStart < 0 || (nestedOpening >= 0 && nestedOpening < closingStart)) continue
    const block = source.slice(contentStart, closingStart)
    if (!isActionColumn(openingTag)) continue
    const buttonCount = (block.match(/<el-button\b/g) || []).length
    const dropdownItemCount = (block.match(/<el-dropdown-item\b/g) || []).length
    const standardized = /<TableActions\b/.test(block)
    const hasMoreMenu =
      (standardized && /#more/.test(block)) ||
      (standardized && /mode=["']menu["']/.test(block)) ||
      /<el-dropdown\b/.test(block)
    const width = numberAttribute(openingTag, 'width')
    const minWidth = numberAttribute(openingTag, 'min-width')
    const effectiveWidth = width ?? minWidth ?? 0
    const requiredWidth = buttonCount * 72 + 32
    const risky =
      buttonCount >= 2 && !hasMoreMenu && (!standardized || effectiveWidth < requiredWidth)
    findings.push({
      file: relative(process.cwd(), file),
      buttonCount,
      dropdownItemCount,
      width: width ?? null,
      minWidth: minWidth ?? null,
      standardized,
      hasMoreMenu,
      risky
    })
  }
}

const riskyFindings = findings
  .filter((item) => item.risky)
  .sort(
    (left, right) => right.buttonCount - left.buttonCount || left.file.localeCompare(right.file)
  )

const report = {
  scannedVueFiles: files.length,
  actionColumns: findings.length,
  riskyActionColumns: riskyFindings.length,
  findings: riskyFindings
}

process.stdout.write(`${JSON.stringify(report, null, 2)}\n`)

if (process.argv.includes('--check') && riskyFindings.length > 0) process.exitCode = 1
