import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('./Dialog.vue', import.meta.url), 'utf8')

test('shared dialogs stay inside the viewport and keep header/footer visible', () => {
  assert.match(source, /max-height: calc\(100vh - 32px\)/)
  assert.match(source, /display: flex;\s*flex-direction: column/)
  assert.match(source, /&__header \{\s*flex: 0 0 auto/)
  assert.match(source, /&__body \{\s*flex: 1 1 auto;\s*min-height: 0;\s*overflow: auto/)
  assert.match(source, /&__footer \{\s*flex: 0 0 auto/)
  assert.match(source, /&\.is-fullscreen \{\s*max-height: 100vh/)
})

test('scroll dialogs shrink to short content and only use fixed height in fullscreen mode', () => {
  assert.match(source, /const scrollbarHeight = computed\(\(\) => \(unref\(isFullscreen\) \? unref\(dialogHeight\) : undefined\)\)/)
  assert.match(source, /const scrollbarMaxHeight = computed\(\(\) => \(unref\(isFullscreen\) \? undefined : unref\(dialogHeight\)\)\)/)
  assert.match(source, /:height="scrollbarHeight"/)
  assert.match(source, /:max-height="scrollbarMaxHeight"/)
  assert.doesNotMatch(source, /<ElScrollbar v-if="scroll" :style="dialogStyle">/)
})
