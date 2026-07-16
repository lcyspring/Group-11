import { readFileSync, readdirSync, statSync } from 'node:fs'
import { relative, resolve } from 'node:path'
import { parse } from 'vue/compiler-sfc'

const root = resolve(process.cwd(), 'src/views')
const files = []

const walk = (directory) => {
  for (const name of readdirSync(directory)) {
    const file = resolve(directory, name)
    if (statSync(file).isDirectory()) walk(file)
    else if (name.endsWith('.vue')) files.push(file)
  }
}

walk(root)

const errors = []
let parsed = 0
for (const file of files) {
  const source = readFileSync(file, 'utf8')
  if (!source.includes('<TableActions')) continue
  parsed += 1
  const result = parse(source, { filename: file })
  for (const error of result.errors) {
    errors.push(`${relative(process.cwd(), file)}: ${String(error)}`)
  }
}

if (errors.length > 0) {
  process.stderr.write(`${errors.join('\n')}\n`)
  process.exitCode = 1
} else {
  process.stdout.write(`Parsed ${parsed} TableActions Vue files without SFC errors.\n`)
}
