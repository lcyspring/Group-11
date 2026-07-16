import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import {
  ReceivableReferenceStatus,
  hasReferenceIssue,
  isContractReferenceInvalid,
  isCustomerReferenceMissing,
  referenceStatusLocaleKey
} from './referenceIntegrity.ts'

test('classifies all receivable customer and contract reference states', () => {
  assert.equal(hasReferenceIssue(ReceivableReferenceStatus.VALID), false)
  assert.equal(hasReferenceIssue(ReceivableReferenceStatus.BOTH_INVALID), true)

  assert.equal(isCustomerReferenceMissing(ReceivableReferenceStatus.CUSTOMER_MISSING), true)
  assert.equal(isCustomerReferenceMissing(ReceivableReferenceStatus.CONTRACT_INVALID), false)
  assert.equal(isContractReferenceInvalid(ReceivableReferenceStatus.CONTRACT_INVALID), true)
  assert.equal(isContractReferenceInvalid(ReceivableReferenceStatus.CUSTOMER_MISSING), false)

  assert.equal(
    referenceStatusLocaleKey(ReceivableReferenceStatus.BOTH_INVALID),
    'receivable.referenceBothInvalid'
  )
})

test('receivable list and detail keep orphan records readable without broken navigation', () => {
  const listSource = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
  const headerSource = readFileSync(
    new URL('./detail/ReceivableDetailsHeader.vue', import.meta.url),
    'utf8'
  )

  assert.match(listSource, /prop="referenceStatus"/)
  assert.match(listSource, /scope\.row\.contract\?\.no/)
  assert.doesNotMatch(listSource, /scope\.row\.contract\.no/)
  assert.match(listSource, /isCustomerReferenceMissing\(scope\.row\.referenceStatus\)/)
  assert.match(listSource, /isContractReferenceInvalid\(scope\.row\.referenceStatus\)/)
  assert.match(headerSource, /receivable\.referenceArchiveNotice/)
})
