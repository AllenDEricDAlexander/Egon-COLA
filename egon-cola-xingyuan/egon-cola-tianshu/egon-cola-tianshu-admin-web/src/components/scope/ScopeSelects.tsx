import { Space } from 'antd'
import AppSelect from './AppSelect'
import BizSelect from './BizSelect'
import EnvSelect from './EnvSelect'
import NamespaceSelect from './NamespaceSelect'

export type ScopeValue = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
}

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
          onChange={(bizCode) => onChange({ ...value, bizCode, namespaceCode: '', env: '', appCode: '' })}
        />
      </span>
      <span style={{ width: 200, display: 'inline-block' }}>
        <NamespaceSelect
          value={value.namespaceCode}
          bizCode={value.bizCode}
          disabled={disabled}
          onChange={(namespaceCode) => onChange({ ...value, namespaceCode, env: '', appCode: '' })}
        />
      </span>
      {includeEnv && (
        <span style={{ width: 140, display: 'inline-block' }}>
          <EnvSelect
            value={value.env}
            bizCode={value.bizCode}
            namespaceCode={value.namespaceCode}
            disabled={disabled}
            onChange={(env) => onChange({ ...value, env, appCode: '' })}
          />
        </span>
      )}
      {includeApp && (
        <span style={{ width: 200, display: 'inline-block' }}>
          <AppSelect
            value={value.appCode}
            bizCode={value.bizCode}
            namespaceCode={value.namespaceCode}
            env={value.env}
            disabled={disabled}
            onChange={(appCode) => onChange({ ...value, appCode })}
          />
        </span>
      )}
    </Space>
  )
}
