import { Select, type SelectProps } from 'antd'
import { useScopeOption } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])

const toValue = (values: string[]): string => values[0] ?? ''

const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function BizSelect({ value, onChange, disabled, placeholder = '请选择或输入业务域' }: Props) {
  const query = useScopeOption('/api/v1/ddc/bizs')
  const bizs = query.data ?? []
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={bizs.map((option) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={query.isFetching}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
