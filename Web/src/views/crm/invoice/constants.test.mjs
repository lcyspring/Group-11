import assert from 'node:assert/strict'
import test from 'node:test'
import {
  INVOICE_DIRECTION,
  INVOICE_STATUS,
  canEditInvoice,
  canIssueInvoice,
  canRedFlushInvoice,
  canVoidInvoice,
  remainingRedAmount,
  toInvoiceEpochMillis
} from './constants.ts'

test('invoice lifecycle helpers cover draft, issued, red and void states', () => {
  assert.equal(canEditInvoice(INVOICE_STATUS.DRAFT, INVOICE_DIRECTION.BLUE), true)
  assert.equal(canEditInvoice(INVOICE_STATUS.ISSUED, INVOICE_DIRECTION.BLUE), false)
  assert.equal(canIssueInvoice(INVOICE_STATUS.DRAFT, INVOICE_DIRECTION.RED), false)
  assert.equal(canRedFlushInvoice(INVOICE_STATUS.ISSUED, INVOICE_DIRECTION.BLUE), true)
  assert.equal(canRedFlushInvoice(INVOICE_STATUS.PARTIALLY_RED, INVOICE_DIRECTION.BLUE), true)
  assert.equal(canRedFlushInvoice(INVOICE_STATUS.FULLY_RED, INVOICE_DIRECTION.BLUE), false)
  assert.equal(canRedFlushInvoice(INVOICE_STATUS.ISSUED, INVOICE_DIRECTION.RED), false)
  assert.equal(canVoidInvoice(INVOICE_STATUS.ISSUED), true)
  assert.equal(canVoidInvoice(INVOICE_STATUS.PARTIALLY_RED), false)
})

test('remaining red amount never becomes negative', () => {
  assert.equal(remainingRedAmount(100, 20), 80)
  assert.equal(remainingRedAmount(0.3, 0.1), 0.2)
  assert.equal(remainingRedAmount(10, 11), 0)
})

test('invoice command dates use epoch milliseconds instead of formatted strings', () => {
  assert.equal(toInvoiceEpochMillis('1784028000000'), 1784028000000)
  assert.equal(toInvoiceEpochMillis(1784028000000.9), 1784028000000)
  assert.throws(() => toInvoiceEpochMillis('2026-07-14 19:20:00'), RangeError)
})
