import type { RoleActivationCandidateView } from '@egon-cola/rbac3-react-sdk'
import { Alert, Card, Checkbox, Space, Tag, Typography } from 'antd'

export interface RoleActivationSelectorProps {
  readonly candidates: RoleActivationCandidateView
  readonly selectedRoleIds: readonly string[]
  readonly disabled: boolean
  readonly onChange: (roleIds: readonly string[]) => void
}

export const findDsdConflict = (
  candidates: RoleActivationCandidateView,
  selectedRoleIds: readonly string[],
): string | null => {
  for (const application of candidates.applications) {
    const selected = application.candidates.filter((candidate) => selectedRoleIds.includes(candidate.rootRoleId))
    const counts = new Map<string, number>()
    selected.forEach((candidate) => candidate.mutexSetIds.forEach((setId) => counts.set(setId, (counts.get(setId) ?? 0) + 1)))
    const conflict = [...counts.entries()].find(([, count]) => count > 1)
    if (conflict) return conflict[0]
  }
  return null
}

export const RoleActivationSelector = ({ candidates, selectedRoleIds, disabled, onChange }: RoleActivationSelectorProps) => {
  const conflict = findDsdConflict(candidates, selectedRoleIds)
  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      {conflict && <Alert type="error" showIcon message="同一 APP 下角色互斥" description={`互斥集合 ${conflict} 中只能激活一个根角色。`} />}
      {candidates.applications.map((application) => (
        <Card key={application.applicationId} size="small" title={`${application.applicationCode} · APP ${application.applicationId}`}>
          <div role="group" aria-label={`${application.applicationCode} 激活角色`}>
            <Space direction="vertical">
              {application.candidates.map((candidate) => (
                <Checkbox
                  key={candidate.rootRoleId}
                  value={candidate.rootRoleId}
                  aria-label={candidate.displayName}
                  checked={selectedRoleIds.includes(candidate.rootRoleId)}
                  disabled={disabled}
                  onChange={(event) => onChange(event.target.checked
                    ? [...selectedRoleIds, candidate.rootRoleId]
                    : selectedRoleIds.filter((roleId) => roleId !== candidate.rootRoleId))}
                >
                  <Space wrap>
                    <Typography.Text strong>{candidate.displayName}</Typography.Text>
                    <Tag>{candidate.rootRoleCode}</Tag>
                    <Tag color={candidate.effectiveFamilyRisk === 'HIGH' ? 'orange' : undefined}>{candidate.effectiveFamilyRisk}</Tag>
                    <Typography.Text type="secondary">
                      子角色 {candidate.sourceRoleIds.join(', ') || '-'} 归一到根角色 {candidate.rootRoleId}
                    </Typography.Text>
                  </Space>
                </Checkbox>
              ))}
            </Space>
          </div>
        </Card>
      ))}
    </Space>
  )
}
