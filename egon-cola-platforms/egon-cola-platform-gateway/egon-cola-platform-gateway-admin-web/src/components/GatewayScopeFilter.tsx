import { Alert, Col, Row, Select, Space, Spin } from 'antd'
import type { GatewayScopeBinding, Scope } from '../api/types'
import { useGatewayScopeBindings } from '../hooks/useGatewayScopeBindings'
import type { ScopeField } from '../hooks/scopeSearchParams'

const labels: Record<ScopeField, string> = {
  bizCode: '业务域',
  namespace: '命名空间',
  env: '环境',
  appCode: '应用',
}

const optionsFor = (
  bindings: GatewayScopeBinding[],
  value: Partial<Scope>,
  field: ScopeField,
): Array<{ value: string; label: string }> => {
  const fields: ScopeField[] = ['bizCode', 'namespace', 'env', 'appCode']
  const index = fields.indexOf(field)
  const values = bindings
    .filter((binding) => fields
      .slice(0, index)
      .every((name) => !value[name] || binding[name] === value[name]))
    .map((binding) => binding[field])
  return [...new Set(values)].sort().map((item) => ({ value: item, label: item }))
}

export type GatewayScopeFilterProps = {
  fields: ScopeField[]
  value: Partial<Scope>
  required?: boolean
  onChange: (value: Partial<Scope>) => void
}

export const GatewayScopeFilter = ({
  fields,
  value,
  required = false,
  onChange,
}: GatewayScopeFilterProps) => {
  const bindings = useGatewayScopeBindings()
  if (bindings.isLoading) return <Spin size="small" />
  if (bindings.error) {
    return (
      <Alert
        type="error"
        showIcon
        message="DDC Scope 加载失败"
        action={<a onClick={() => void bindings.refetch()}>重试</a>}
      />
    )
  }
  const available = bindings.data ?? []
  if (!available.length) {
    return <Alert type="info" showIcon message="暂无可用 DDC Scope Binding" />
  }
  return (
    <Row gutter={[8, 8]}>
      {fields.map((field) => (
        <Col key={field} xs={24} sm={12} md={6}>
          <Select
            allowClear
            style={{ width: '100%' }}
            aria-label={labels[field]}
            placeholder={`${labels[field]}${required ? '（必选）' : ''}`}
            value={value[field] || undefined}
            options={optionsFor(available, value, field)}
            onChange={(selected) => {
              const next = { ...value, [field]: selected ?? '' }
              fields.slice(fields.indexOf(field) + 1).forEach((later) => {
                if (later !== field) delete next[later]
              })
              onChange(next)
            }}
          />
        </Col>
      ))}
      <Col flex="auto">
        <Space />
      </Col>
    </Row>
  )
}
