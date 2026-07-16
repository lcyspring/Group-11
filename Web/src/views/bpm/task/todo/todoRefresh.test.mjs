import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (url) => new TextDecoder('utf-8', { fatal: true }).decode(readFileSync(url))

const todo = read(new URL('./index.vue', import.meta.url))
const done = read(new URL('../done/index.vue', import.meta.url))
const crmBacklog = read(
  new URL('../../../crm/backlog/components/BpmTaskBacklogList.vue', import.meta.url)
)

test('cached BPM task lists reload after returning from an approval detail', () => {
  for (const source of [todo, done, crmBacklog]) {
    assert.match(source, /let initialized = false/)
    assert.match(source, /onMounted\(async \(\) => \{[\s\S]*?initialized = true[\s\S]*?\}\)/)
    assert.match(
      source,
      /onActivated\(\(\) => \{[\s\S]*?if \(initialized\) (?:getList|loadData)\(\)/
    )
  }
})
