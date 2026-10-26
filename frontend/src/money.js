export function toCents(value) {
  const match = /^(\d+)(?:\.(\d{1,2}))?$/.exec(String(value))
  if (!match) throw new Error('金额必须为最多两位小数的非负十进制数')
  return BigInt(match[1]) * 100n + BigInt((match[2] || '').padEnd(2, '0'))
}

export function formatCents(value) {
  const cents = BigInt(value)
  return `¥${(cents / 100n).toLocaleString('zh-CN')}.${String(cents % 100n).padStart(2, '0')}`
}

export const money = value => formatCents(toCents(value))
export const lineCents = (price, quantity) => toCents(price) * BigInt(Number.isInteger(quantity) && quantity > 0 ? quantity : 0)
