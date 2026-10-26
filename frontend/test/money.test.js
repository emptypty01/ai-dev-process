import { test } from 'node:test'
import assert from 'node:assert/strict'
import { toCents, formatCents, lineCents } from '../src/money.js'

test('configured prices produce the confirmed 687.00 order', () => {
  assert.equal(formatCents(lineCents('299.00', 2) + lineCents('89.00', 1)), '¥687.00')
  assert.equal(formatCents(lineCents('0.10', 3)), '¥0.30')
})

test('large decimal string keeps the last cent beyond Number safe integers', () => {
  assert.equal(toCents('99999999999999.99'), 9999999999999999n)
  assert.equal(formatCents(toCents('99999999999999.99')), '¥99,999,999,999,999.99')
})

test('fractional quantities cannot become a partial item preview', () => {
  assert.equal(lineCents('299.00', 1.5), 0n)
  assert.throws(() => toCents('1.001'))
})
