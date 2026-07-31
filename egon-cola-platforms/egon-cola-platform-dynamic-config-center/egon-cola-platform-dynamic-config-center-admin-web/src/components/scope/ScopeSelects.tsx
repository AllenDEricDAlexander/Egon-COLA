import { Space } from 'antd'
import AppSelect from './AppSelect'
import BizSelect from './BizSelect'
import EnvSelect from './EnvSelect'
import NamespaceSelect from './NamespaceSelect'

export type ScopeValue = { bizCode: string; appCode: string; env: string; namespace: string }

type Props = {
  value: ScopeValue
  onChange: (value: ScopeValue) => void
  includeApp?: boolean
  includeEnv?: boolean
  disabled?: boolean
}

export default function ScopeSelects({
  value,
  onChange,
  includeApp = true,
  includeEnv = true,
  disabled = false,
}: Props) {
  return (
    <Space wrap>
      <span style={{ width: 200, display: 'inline-block' }}>
        <BizSelect
          value={value.bizCode}
          disabled={disabled}
          onChange={(bizCode) => onChange({ ...value, bizCode, appCode: '', namespace: '' })}
        />
      </span>
      {includeApp && (
        <span style={{ width: 200, display: 'inline-block' }}>
          <AppSelect
            value={value.appCode}
            biz={value.bizCode}
            disabled={disabled}
            onChange={(appCode) => onChange({ ...value, appCode, namespace: '' })}
          />
        </span>
      )}
      <span style={{ width: 200, display: 'inline-block' }}>
        <NamespaceSelect
          value={value.namespace}
          appCode={value.appCode}
          disabled={disabled}
          onChange={(namespace) => onChange({ ...value, namespace })}
        />
      </span>
      {includeEnv && (
        <span style={{ width: 140, display: 'inline-block' }}>
          <EnvSelect
            value={value.env}
            disabled={disabled}
            onChange={(env) => onChange({ ...value, env })}
          />
        </span>
      )}
    </Space>
  )
}
