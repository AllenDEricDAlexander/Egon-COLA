import { Tag } from 'antd'

const success = new Set(['SUCCESS', 'SUCCEEDED', 'READY', 'ACTIVE', 'CONSISTENT', 'APPLIED'])
const danger = new Set(['FAILED', 'TIMEOUT', 'UNKNOWN', 'NOT_READY', 'INCONSISTENT', 'REJECTED'])
const processing = new Set(['PENDING', 'RUNNING', 'PUBLISHING', 'APPLYING', 'RETRYING'])

export const isSuccessfulStatus = (status: string, partialApplied = false): boolean =>
  !partialApplied && success.has(status.toUpperCase())

export const StatusTag = ({
  status,
  partialApplied = false,
}: {
  status: string
  partialApplied?: boolean
}) => {
  const normalized = status.toUpperCase()
  const color = partialApplied
    ? 'error'
    : success.has(normalized)
      ? 'success'
      : danger.has(normalized)
        ? 'error'
        : processing.has(normalized)
          ? 'processing'
          : 'default'
  return (
    <Tag color={color}>
      {partialApplied ? `${status}（部分生效）` : status || '不支持的状态'}
    </Tag>
  )
}
