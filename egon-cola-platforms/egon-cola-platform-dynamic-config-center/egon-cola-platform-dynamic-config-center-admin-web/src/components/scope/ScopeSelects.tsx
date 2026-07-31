import { Space } from 'antd'
import AppSelect from './AppSelect'
import EnvSelect from './EnvSelect'
import NamespaceSelect from './NamespaceSelect'

export type ScopeValue = { appCode: string; env: string; namespace: string }

type Props = {
  value: ScopeValue
  onChange: (value: ScopeValue) => void
  includeApp?: boolean
  disabled?: boolean
}

export default function ScopeSelects({ value, onChange, includeApp = true, disabled = false }: Props) {
  return (
    <Space wrap>
      <span style={{ width: 200, display: 'inline-block' }}>
        <NamespaceSelect
          value={value.namespace}
          disabled={disabled}
          onChange={(namespace) => onChange({ ...value, namespace, appCode: '' })}
        />
      </span>
      {includeApp && (
        <span style={{ width: 200, display: 'inline-block' }}>
          <AppSelect
            value={value.appCode}
            namespace={value.namespace}
            disabled={disabled}
            onChange={(appCode) => onChange({ ...value, appCode })}
          />
        </span>
      )}
      <span style={{ width: 140, display: 'inline-block' }}>
        <EnvSelect
          value={value.env}
          disabled={disabled}
          onChange={(env) => onChange({ ...value, env })}
        />
      </span>
    </Space>
  )
}
