import { Select, type SelectProps } from 'antd'
import { useScopeOptions, type ScopeOption } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  bizCode?: string
  namespaceCode?: string
  env?: string
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])

const toValue = (values: string[]): string => values[0] ?? ''

const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function AppSelect({ value, onChange, bizCode = '', namespaceCode = '', env = '', disabled, placeholder = '请选择或输入应用' }: Props) {
  const { apps, loading } = useScopeOptions(bizCode, namespaceCode, env)
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={apps.map((option: ScopeOption) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
