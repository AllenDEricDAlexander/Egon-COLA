import { Select, type SelectProps } from 'antd'
import { useScopeOption, withParams } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  bizCode?: string
  namespaceCode?: string
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])

const toValue = (values: string[]): string => values[0] ?? ''

const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function EnvSelect({ value, onChange, bizCode = '', namespaceCode = '', disabled, placeholder = '请选择或输入环境' }: Props) {
  const query = useScopeOption(withParams(
    '/api/v1/ddc/envs',
    { bizCode, namespaceCode },
  ))
  const envs = query.data ?? []
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={envs.map((option) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={query.isFetching}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
