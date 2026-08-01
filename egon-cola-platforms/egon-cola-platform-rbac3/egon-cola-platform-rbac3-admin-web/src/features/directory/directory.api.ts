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

export const directoryApi = (client: FeatureApiClient) => ({
  user: (userId: string) => client.request<UserDirectoryView>(
    `/api/rbac3/v1/directory/users/${encodeURIComponent(userId)}`,
  ),
  submitSnapshot: (command: DirectorySnapshotCommand) => client.request<DirectorySyncView>(
    '/api/rbac3/v1/directory/snapshots',
    { method: 'POST', body: command },
  ),
})
