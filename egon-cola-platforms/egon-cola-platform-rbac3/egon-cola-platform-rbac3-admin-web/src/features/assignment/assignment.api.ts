import type { FeatureApiClient } from '../shared/FeatureApi'

export interface AssignmentView {
  readonly assignmentId: string
  readonly roleId: string
  readonly assignmentType: string
  readonly status: string
  readonly validFrom: string
  readonly validTo: string | null
  readonly sourceType: string
  readonly sourceId: string
  readonly version: number
}

export interface CreateAssignmentCommand {
  readonly roleId: string
  readonly validFrom: string
  readonly validTo: string | null
  readonly assignmentType: string
  readonly reason: string | null
  readonly ticketNo: string | null
  readonly expectedUserAuthVersion: number
}

export interface ChangeAssignmentCommand {
  readonly reason: string | null
  readonly ticketNo: string | null
  readonly expectedAssignmentVersion: number
  readonly expectedUserAuthVersion: number
}

export type AssignmentOperation = 'revoke' | 'suspend' | 'resume'

export const assignmentApi = (client: FeatureApiClient) => ({
  list: (userId: string) => client.request<readonly AssignmentView[]>(
    `/api/rbac3/v1/users/${encodeURIComponent(userId)}/role-assignments`,
  ),
  create: (userId: string, command: CreateAssignmentCommand, idempotencyKey: string) => client.request(
    `/api/rbac3/v1/users/${encodeURIComponent(userId)}/role-assignments`,
    { method: 'POST', body: command, headers: { 'Idempotency-Key': idempotencyKey } },
  ),
  change: (
    userId: string,
    assignmentId: string,
    operation: AssignmentOperation,
    command: ChangeAssignmentCommand,
    idempotencyKey: string,
  ) => client.request(
    `/api/rbac3/v1/users/${encodeURIComponent(userId)}/role-assignments/${encodeURIComponent(assignmentId)}/${operation}`,
    { method: 'POST', body: command, headers: { 'Idempotency-Key': idempotencyKey } },
  ),
})
