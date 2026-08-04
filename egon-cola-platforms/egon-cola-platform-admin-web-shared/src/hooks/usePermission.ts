import { useMemo } from 'react'

export const usePermission = (permissions: readonly string[]) => {
  const set = useMemo(() => new Set(permissions), [permissions])
  return useMemo(() => ({
    has: (permission: string) => set.has(permission),
    hasAll: (...required: string[]) => required.every((p) => set.has(p)),
    hasAny: (...required: string[]) => required.some((p) => set.has(p)),
  }), [set])
}
