import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import { dirname, extname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import { isDialogCancellation, resolveDialogAction } from './dialogAction.ts'

const sourceRoot = dirname(fileURLToPath(import.meta.url))
const crmRoot = join(sourceRoot, '..', 'views', 'crm')

const collectVueFiles = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true })
  const nested = await Promise.all(
    entries.map((entry) => {
      const path = join(directory, entry.name)
      return entry.isDirectory() ? collectVueFiles(path) : extname(entry.name) === '.vue' ? [path] : []
    })
  )
  return nested.flat()
}

test('dialog cancellation only accepts explicit Element Plus cancel actions', async () => {
  assert.equal(isDialogCancellation('cancel'), true)
  assert.equal(isDialogCancellation('close'), true)
  assert.equal(isDialogCancellation('confirm'), false)
  assert.equal(isDialogCancellation(new Error('cancel')), false)
  assert.equal(await resolveDialogAction(Promise.resolve('confirm')), 'confirm')
  assert.equal(await resolveDialogAction(Promise.reject('cancel')), undefined)
  await assert.rejects(resolveDialogAction(Promise.reject(new Error('request failed'))), /request failed/)
})

test('CRM Vue components do not silently swallow dialog and request failures', async () => {
  const files = await collectVueFiles(crmRoot)
  const sources = await Promise.all(files.map((path) => readFile(path, 'utf8')))
  const offenders = files.filter((_, index) => /catch\s*(?:\([^)]*\))?\s*\{\s*\}/s.test(sources[index]))
  assert.deepEqual(offenders, [])
})
