import {useQuery} from '@tanstack/react-query'
import {Select} from 'antd'
import {useFeatureApi} from '../shared/FeatureApi'
import {applicationApi} from './application.api'

export interface PermissionSelectorProps {
  readonly applicationId: string
  readonly value?: string
  readonly onChange?: (value: string) => void
}

/** Advanced mapping-only selector; ordinary resource and role screens never render permission characters. */
export const PermissionSelector = ({applicationId, value, onChange}: PermissionSelectorProps) => {
  const api = applicationApi(useFeatureApi())
  const permissions = useQuery({
    queryKey: ['rbac3', 'assignable-permissions', applicationId],
    queryFn: () => api.permissions(applicationId, true),
    enabled: applicationId.length > 0,
  })
  return (
    <Select
      showSearch
      loading={permissions.isPending}
      value={value}
      placeholder="选择实际权限字符"
      optionFilterProp="label"
      options={(permissions.data ?? [])
        .filter((permission) => permission.status === 'ACTIVE')
        .map((permission) => ({value: permission.id, label: `${permission.permissionName} (${permission.permissionCode})`}))}
      onChange={onChange}
    />
  )
}
