import type {ReactNode} from 'react'
import {useRbac3Authorization} from '../hooks/useRbac3Authorization'

export interface FieldGuardProps {
  readonly policyKey: string
  readonly fieldCode: string
  readonly value?: ReactNode
  readonly maskedValue?: ReactNode
  readonly fallback?: ReactNode
  readonly render?: (value: ReactNode) => ReactNode
}

/** Renders only server-authorized values; masking must already be done server-side. */
export const FieldGuard = ({
  policyKey,
  fieldCode,
  value = null,
  maskedValue = null,
  fallback = null,
  render,
}: FieldGuardProps) => {
    const {status, bootstrap} = useRbac3Authorization()
  if (status !== 'READY' || bootstrap === null) {
    return fallback
  }
  const policy = bootstrap.fieldPolicies[policyKey]
  const access = policy?.decision === 'ALLOW' ? policy.fields[fieldCode] : undefined
  if (access === undefined || access.level === 'NONE') {
    return fallback
  }
  const authorizedValue = access.level === 'MASKED_READ' ? maskedValue : value
  if (authorizedValue === null || authorizedValue === undefined) {
    return fallback
  }
  return render ? render(authorizedValue) : authorizedValue
}
