import type { FeatureApiClient } from '../shared/FeatureApi'

export interface ApplicationView {
  readonly applicationId: string
  readonly applicationCode: string
  readonly applicationName: string
  readonly status: string
  readonly version: number
}

export interface ResourceView {
  readonly resourceId: string
  readonly applicationId: string
  readonly resourceType: string
  readonly resourceCode: string
  readonly resourceName: string
  readonly parentResourceId?: string | null
  readonly requiredPermissionId?: string | null
  readonly status: string
  readonly version: number
}

export interface ManifestView {
  readonly manifestId: string
  readonly applicationId: string
  readonly status: string
  readonly checksum: string
  readonly manifestVersion: number
}

export interface ManifestValidationView {
  readonly manifestId: string
  readonly valid: boolean
  readonly errors: readonly string[]
  readonly warnings: readonly string[]
}

export interface ManifestImpactView {
  readonly manifestId: string
  readonly resourcesAdded: number
  readonly resourcesChanged: number
  readonly resourcesStale: number
  readonly affectedRoleCount: number
  readonly conflicts: readonly string[]
}

export interface ActivateManifestCommand {
  readonly applicationId: string
  readonly expectedApplicationVersion: number
  readonly expectedCurrentManifestVersion: number
  readonly expectedDefinitionSetId: string
  readonly reason: string
  readonly idempotencyKey: string
}

export interface ManifestActivationView {
  readonly manifestId: string
  readonly policyVersion: number
  readonly propagationId: string
  readonly propagationPending: boolean
}

export const applicationApi = (client: FeatureApiClient) => ({
  applications: () => client.request<readonly ApplicationView[]>('/api/rbac3/v1/applications'),
  resources: (applicationId: string) => client.request<readonly ResourceView[]>(
    `/api/rbac3/v1/applications/${encodeURIComponent(applicationId)}/resources`,
  ),
  manifest: (manifestId: string) => client.request<ManifestView>(
    `/api/rbac3/v1/resource-manifests/${encodeURIComponent(manifestId)}`,
  ),
  validation: (manifestId: string) => client.request<ManifestValidationView>(
    `/api/rbac3/v1/resource-manifests/${encodeURIComponent(manifestId)}/validation`,
  ),
  impact: (manifestId: string) => client.request<ManifestImpactView>(
    `/api/rbac3/v1/resource-manifests/${encodeURIComponent(manifestId)}/impact-analysis`,
    { method: 'POST' },
  ),
  activate: (manifestId: string, command: ActivateManifestCommand) => client.request<ManifestActivationView>(
    `/api/rbac3/v1/resource-manifests/${encodeURIComponent(manifestId)}/activate`,
    {
      method: 'POST',
      headers: {
        'If-Match': String(command.expectedApplicationVersion),
        'Idempotency-Key': command.idempotencyKey,
      },
      body: {
        applicationId: command.applicationId,
        expectedCurrentManifestVersion: command.expectedCurrentManifestVersion,
        expectedDefinitionSetId: command.expectedDefinitionSetId,
        reason: command.reason,
      },
    },
  ),
  archive: (resource: ResourceView) => client.request<{ readonly status: string }>(
    `/api/rbac3/v1/resources/${encodeURIComponent(resource.resourceId)}/archive`,
    { method: 'POST', body: { expectedVersion: resource.version } },
  ),
})
