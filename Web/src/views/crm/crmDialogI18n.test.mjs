import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const crmRoot = fileURLToPath(new URL('.', import.meta.url))

const collectVueFiles = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true })
  const nested = await Promise.all(
    entries.map((entry) => {
      const target = path.join(directory, entry.name)
      return entry.isDirectory()
        ? collectVueFiles(target)
        : Promise.resolve(entry.name.endsWith('.vue') ? [target] : [])
    })
  )
  return nested.flat()
}

test('CRM dialogs never render unresolved dialog confirm or cancel keys', async () => {
  const files = await collectVueFiles(crmRoot)
  const violations = []
  for (const file of files) {
    const source = await readFile(file, 'utf8')
    if (/dialog\.(?:confirm|cancel)/.test(source)) {
      violations.push(path.relative(crmRoot, file))
    }
  }
  assert.deepEqual(violations, [])
  assert.ok(files.length >= 160, `expected the CRM tree, only scanned ${files.length} Vue files`)
})
