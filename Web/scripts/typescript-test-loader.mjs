import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'

/**
 * Minimal ESM loader for Node's built-in test runner.
 *
 * Ubuntu's Node package can be built without the experimental native
 * TypeScript stripper. The project already depends on TypeScript, so tests
 * transpile imported .ts modules through the same pinned compiler instead of
 * relying on a distribution-specific Node build option.
 */
export async function load(url, context, nextLoad) {
  if (!url.startsWith('file:') || !url.endsWith('.ts')) {
    return nextLoad(url, context)
  }
  const source = await readFile(fileURLToPath(url), 'utf8')
  const result = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.ESNext,
      target: ts.ScriptTarget.ES2022,
      sourceMap: true
    },
    fileName: fileURLToPath(url)
  })
  return {
    format: 'module',
    source: result.outputText,
    shortCircuit: true
  }
}
