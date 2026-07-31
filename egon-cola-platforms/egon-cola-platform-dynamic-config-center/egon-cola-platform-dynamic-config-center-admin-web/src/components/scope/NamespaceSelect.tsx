import { Select, type SelectProps } from 'antd'
import { useScopeOptions } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  appCode?: string
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])

const toValue = (values: string[]): string => values[0] ?? ''

const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function NamespaceSelect({ value, onChange, appCode = '', disabled, placeholder = '请选择或输入命名空间' }: Props) {
  const { namespaces, loading } = useScopeOptions('', appCode)
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={namespaces.map((option) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
