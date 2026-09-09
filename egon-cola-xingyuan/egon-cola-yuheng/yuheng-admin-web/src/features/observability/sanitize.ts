const sensitive = /(authorization|credential|cookie|secret|token|password|requestbody|responsebody)/i

export const sanitizeForDisplay = (value: unknown): unknown => {
  if (Array.isArray(value)) {
    return value.map(sanitizeForDisplay)
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .filter(([key]) => !sensitive.test(key))
        .map(([key, nested]) => [key, sanitizeForDisplay(nested)]),
    )
  }
  return value
}
