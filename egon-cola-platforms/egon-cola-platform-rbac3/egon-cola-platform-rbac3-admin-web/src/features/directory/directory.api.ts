import type { FeatureApiClient } from '../shared/FeatureApi'

export interface UserDirectoryView {
  readonly userId: string
  readonly username: string
  readonly displayName: string
  readonly status: string
  readonly authVersion: number
  readonly directorySnapshotVersion: number
}

export interface DirectorySnapshotCommand {
  readonly providerCode: string
  readonly snapshotVersion: number
  readonly checksum: string
  readonly generatedAt: string
  readonly payload: Readonly<Record<string, unknown>>
}

export interface DirectorySyncView {
  readonly snapshotId: string
  readonly outcome: string
  readonly counts: Readonly<Record<string, number>>
  readonly affectedUserCount: number
}

export interface OrganizationView {
  readonly orgUnitId: string
  readonly snapshotId: string | null
  readonly type: string
  readonly code: string
  readonly name: string
  readonly parentId: string | null
  readonly path: string
  readonly depth: number
  readonly status: string
}

export interface PositionView {
  readonly positionId: string
  readonly snapshotId: string | null
  readonly code: string
  readonly name: string
  readonly orgUnitId: string
  readonly status: string
}

export const directoryApi = (client: FeatureApiClient) => ({
  user: (userId: string) => client.request<UserDirectoryView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}`,
  ),
  organizations: (parentId?: string) => client.request<readonly OrganizationView[]>(
    '/api/rbac3/v1/iam/organizations',
    {query: {parentId}},
  ),
  positions: (orgUnitId?: string) => client.request<readonly PositionView[]>(
    '/api/rbac3/v1/iam/positions',
    {query: {orgUnitId}},
  ),
  submitSnapshot: (command: DirectorySnapshotCommand) => client.request<DirectorySyncView>(
    '/api/rbac3/v1/internal/directory-snapshots',
    { method: 'POST', body: command },
  ),
})
