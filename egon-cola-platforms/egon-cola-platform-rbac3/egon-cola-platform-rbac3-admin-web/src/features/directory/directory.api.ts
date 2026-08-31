import type { FeatureApiClient } from '../shared/FeatureApi'

export interface UserDirectoryView {
  readonly userId: string
  readonly identitySub: string
  readonly username?: string
  readonly displayName?: string
  readonly status: string
  readonly authVersion: number
  readonly directorySnapshotVersion?: number
}

export interface DirectoryPage<T> {
  readonly items: readonly T[]
  readonly page: number
  readonly size: number
  readonly total: number
}

export interface UserDirectoryFilter {
  readonly query?: string
  readonly status?: string
  readonly orgUnitId?: string
  readonly positionId?: string
  readonly page?: number
  readonly size?: number
}

export interface CreateUserCommand {
  readonly identitySub: string
  readonly status: string
}

export interface UpdateUserCommand {
  readonly identitySub: string
  readonly expectedAuthVersion: number
}

export interface UserStatusCommand {
  readonly status: string
  readonly reason: string
  readonly expectedAuthVersion: number
}

export interface OrganizationCommand {
  readonly type: string
  readonly code?: string
  readonly name: string
  readonly parentId: string | number | null
  readonly externalId?: string | null
  readonly validFrom: string
  readonly validTo: string | null
}

export interface OrganizationUpdateCommand {
  readonly type: string
  readonly name: string
  readonly parentId: string | number | null
  readonly externalId?: string | null
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface PositionCommand {
  readonly code?: string
  readonly name: string
  readonly orgUnitId: string | number
  readonly externalId?: string | null
  readonly validFrom: string
  readonly validTo: string | null
}

export interface PositionUpdateCommand {
  readonly name: string
  readonly orgUnitId: string | number
  readonly externalId?: string | null
  readonly validFrom: string
  readonly validTo: string | null
  readonly expectedVersion: number
}

export interface UserOrganizationAssignmentView {
  readonly assignmentId: string
  readonly userId: string
  readonly orgUnitId: string
  readonly status: string
  readonly validFrom: string
  readonly validTo: string | null
  readonly sourceType: string
  readonly sourceId: string | null
  readonly reason: string | null
  readonly ticketNo: string | null
  readonly version: number
}

export interface UserPositionAssignmentView extends UserOrganizationAssignmentView {
  readonly positionId: string
  readonly primaryAssignment: boolean
}

export interface OrganizationAssignmentCommand {
  readonly orgUnitId: string | number
  readonly validFrom: string
  readonly validTo: string | null
  readonly reason: string
  readonly ticketNo: string
}

export interface PositionAssignmentCommand extends OrganizationAssignmentCommand {
  readonly positionId: string | number
  readonly primaryAssignment: boolean
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
  readonly externalId: string | null
  readonly validFrom: string
  readonly validTo: string | null
  readonly version: number
}

export interface PositionView {
  readonly positionId: string
  readonly snapshotId: string | null
  readonly code: string
  readonly name: string
  readonly orgUnitId: string
  readonly status: string
  readonly externalId: string | null
  readonly validFrom: string
  readonly validTo: string | null
  readonly version: number
}

const normalizeId = (value: string | number): string | number => (
  typeof value === 'string' ? value.trim() : value
)

const normalizeOrganizationAssignment = (command: OrganizationAssignmentCommand): OrganizationAssignmentCommand => ({
  orgUnitId: normalizeId(command.orgUnitId),
  validFrom: command.validFrom,
  validTo: command.validTo || null,
  reason: command.reason.trim(),
  ticketNo: command.ticketNo.trim(),
})

const normalizePositionAssignment = (command: PositionAssignmentCommand): PositionAssignmentCommand => ({
  ...normalizeOrganizationAssignment(command),
  positionId: normalizeId(command.positionId),
  primaryAssignment: command.primaryAssignment,
})

export const directoryApi = (client: FeatureApiClient) => ({
  users: (filters: UserDirectoryFilter = {}) => client.request<DirectoryPage<UserDirectoryView>>(
    '/api/rbac3/v1/iam/users',
    { query: {
      query: filters.query,
      status: filters.status,
      orgUnitId: filters.orgUnitId,
      positionId: filters.positionId,
      page: filters.page,
      size: filters.size,
    } },
  ),
  createUser: (command: CreateUserCommand) => client.request<UserDirectoryView>(
    '/api/rbac3/v1/iam/users',
    { method: 'POST', body: command },
  ),
  user: (userId: string) => client.request<UserDirectoryView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}`,
  ),
  updateUser: (userId: string, command: UpdateUserCommand) => client.request<UserDirectoryView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}`,
    { method: 'PUT', body: command },
  ),
  deleteUser: (userId: string, expectedAuthVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}`,
    { method: 'DELETE', query: { expectedAuthVersion } },
  ),
  changeUserStatus: (userId: string, command: UserStatusCommand) => client.request<UserDirectoryView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/status`,
    { method: 'PUT', body: command },
  ),
  organizations: (parentId?: string) => client.request<readonly OrganizationView[]>(
    '/api/rbac3/v1/iam/organizations',
    {query: {parentId}},
  ),
  createOrganization: (command: OrganizationCommand) => client.request<OrganizationView>(
    '/api/rbac3/v1/iam/organizations',
    { method: 'POST', body: command },
  ),
  updateOrganization: (orgUnitId: string, command: OrganizationUpdateCommand) => client.request<OrganizationView>(
    `/api/rbac3/v1/iam/organizations/${encodeURIComponent(orgUnitId)}`,
    { method: 'PUT', body: command },
  ),
  deleteOrganization: (orgUnitId: string, expectedVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/organizations/${encodeURIComponent(orgUnitId)}`,
    { method: 'DELETE', query: { expectedVersion } },
  ),
  positions: (orgUnitId?: string) => client.request<readonly PositionView[]>(
    '/api/rbac3/v1/iam/positions',
    {query: {orgUnitId}},
  ),
  createPosition: (command: PositionCommand) => client.request<PositionView>(
    '/api/rbac3/v1/iam/positions',
    { method: 'POST', body: command },
  ),
  updatePosition: (positionId: string, command: PositionUpdateCommand) => client.request<PositionView>(
    `/api/rbac3/v1/iam/positions/${encodeURIComponent(positionId)}`,
    { method: 'PUT', body: command },
  ),
  deletePosition: (positionId: string, expectedVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/positions/${encodeURIComponent(positionId)}`,
    { method: 'DELETE', query: { expectedVersion } },
  ),
  organizationAssignments: (userId: string) => client.request<readonly UserOrganizationAssignmentView[]>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/organizations`,
  ),
  assignOrganization: (userId: string, command: OrganizationAssignmentCommand) => client.request<UserOrganizationAssignmentView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/organizations`,
    { method: 'POST', body: normalizeOrganizationAssignment(command) },
  ),
  revokeOrganization: (userId: string, assignmentId: string, expectedVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/organizations/${encodeURIComponent(assignmentId)}`,
    { method: 'DELETE', query: { expectedVersion } },
  ),
  positionAssignments: (userId: string) => client.request<readonly UserPositionAssignmentView[]>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/positions`,
  ),
  assignPosition: (userId: string, command: PositionAssignmentCommand) => client.request<UserPositionAssignmentView>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/positions`,
    { method: 'POST', body: normalizePositionAssignment(command) },
  ),
  revokePosition: (userId: string, assignmentId: string, expectedVersion: number) => client.request<null>(
    `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/positions/${encodeURIComponent(assignmentId)}`,
    { method: 'DELETE', query: { expectedVersion } },
  ),
  submitSnapshot: (command: DirectorySnapshotCommand) => client.request<DirectorySyncView>(
    '/api/rbac3/v1/internal/directory-snapshots',
    { method: 'POST', body: command },
  ),
})
