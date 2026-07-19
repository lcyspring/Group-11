import assert from 'node:assert/strict'
import { readFile, readdir, writeFile } from 'node:fs/promises'
import { dirname, extname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { IconJson } from '../src/components/Icon/src/data.ts'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const webRoot = resolve(scriptDir, '..')
const projectRoot = resolve(webRoot, '..')
const outputFile = resolve(webRoot, 'src/components/Icon/src/offline-icon-collections.generated.json')
const sourceExtensions = new Set(['.js', '.mjs', '.ts', '.tsx', '.vue', '.sql'])
const mode = Deno.args[0]

assert.ok(mode === '--check' || mode === '--write', 'usage: generate-offline-icons.mjs --check|--write')

async function* walk(path) {
  for (const entry of await readdir(path, { withFileTypes: true })) {
    const child = resolve(path, entry.name)
    if (entry.isDirectory()) {
      yield* walk(child)
    } else if (entry.isFile() && sourceExtensions.has(extname(entry.name))) {
      yield child
    }
  }
}

async function discoverExplicitIcons() {
  const result = new Map()
  for (const root of [resolve(webRoot, 'src'), resolve(projectRoot, 'database')]) {
    for await (const file of walk(root)) {
      const source = await readFile(file, 'utf8')
      for (const line of source.split(/\r?\n/)) {
        if (!/(?:\bicon\b|<Icon)/i.test(line)) continue
        for (const match of line.matchAll(/['"]([a-z0-9-]+):([a-z0-9][a-z0-9-]*)['"]/g)) {
          const [, prefix, name] = match
          if (!result.has(prefix)) result.set(prefix, new Set())
          result.get(prefix).add(name)
        }
      }
    }
  }
  return result
}

function sortRecord(record = {}) {
  return Object.fromEntries(Object.entries(record).sort(([left], [right]) => left.localeCompare(right)))
}

function normalizeCollection(collection) {
  const normalized = {
    prefix: collection.prefix,
    icons: sortRecord(collection.icons)
  }
  if (collection.aliases && Object.keys(collection.aliases).length > 0) {
    normalized.aliases = sortRecord(collection.aliases)
  }
  if (collection.width) normalized.width = collection.width
  if (collection.height) normalized.height = collection.height
  return normalized
}

async function fetchJson(url) {
  const response = await fetch(url)
  assert.ok(response.ok, `Iconify request failed (${response.status}): ${url}`)
  return response.json()
}

async function writeSnapshot(discovered) {
  const catalog = await fetchJson('https://api.iconify.design/collections')
  const requested = new Map([...discovered].map(([prefix, names]) => [prefix, new Set(names)]))
  for (const [selectorPrefix, names] of Object.entries(IconJson)) {
    const prefix = selectorPrefix.slice(0, -1)
    if (!requested.has(prefix)) requested.set(prefix, new Set())
    for (const name of names) requested.get(prefix).add(name)
  }
  const prefixes = [...requested.keys()]
    .filter((prefix) => Object.hasOwn(catalog, prefix))
    .sort()
  const collections = []
  const missingIcons = []

  for (const prefix of prefixes) {
    const names = [...requested.get(prefix)].sort()
    const merged = { prefix, icons: {}, aliases: {} }
    for (let offset = 0; offset < names.length; offset += 50) {
      const chunk = names.slice(offset, offset + 50)
      const url = `https://api.iconify.design/${prefix}.json?icons=${encodeURIComponent(chunk.join(','))}`
      const collection = await fetchJson(url)
      assert.equal(collection.prefix, prefix, `unexpected Iconify prefix for ${prefix}`)
      missingIcons.push(...(collection.not_found || []).map((name) => `${prefix}:${name}`))
      Object.assign(merged.icons, collection.icons)
      Object.assign(merged.aliases, collection.aliases)
      if (collection.width) merged.width = collection.width
      if (collection.height) merged.height = collection.height
    }
    collections.push(normalizeCollection(merged))
  }

  assert.deepEqual(missingIcons, [], 'IconSelect or source contains icons absent from Iconify')

  await writeFile(outputFile, `${JSON.stringify(collections, null, 2)}\n`)
  console.log(`Wrote ${collections.length} offline Iconify collections to ${outputFile}`)
}

function collectionContains(collection, name) {
  return Object.hasOwn(collection.icons, name) || Object.hasOwn(collection.aliases || {}, name)
}

async function checkSnapshot(discovered) {
  const collections = JSON.parse(await readFile(outputFile, 'utf8'))
  const byPrefix = new Map(collections.map((collection) => [collection.prefix, collection]))

  for (const [selectorPrefix, names] of Object.entries(IconJson)) {
    const prefix = selectorPrefix.slice(0, -1)
    const collection = byPrefix.get(prefix)
    assert.ok(collection, `offline collection missing for IconSelect prefix: ${prefix}`)
    for (const name of names) {
      assert.ok(collectionContains(collection, name), `offline IconSelect icon missing: ${prefix}:${name}`)
    }
  }

  for (const [prefix, names] of discovered) {
    if (prefix === 'svg-icon') continue
    const collection = byPrefix.get(prefix)
    assert.ok(collection, `offline collection missing for explicit icon prefix: ${prefix}`)
    for (const name of names) {
      assert.ok(collectionContains(collection, name), `offline explicit icon missing: ${prefix}:${name}`)
    }
  }

  const iconCount = collections.reduce(
    (total, collection) => total + Object.keys(collection.icons).length + Object.keys(collection.aliases || {}).length,
    0
  )
  console.log(`Offline Iconify snapshot passed: collections=${collections.length} icons=${iconCount}`)
}

const discovered = await discoverExplicitIcons()
if (mode === '--write') {
  await writeSnapshot(discovered)
}
await checkSnapshot(discovered)
