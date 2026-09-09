import type {FeatureApiClient} from '../shared/FeatureApi'

export interface SimulationDecision {
  readonly decision: string
  readonly reasonCode: string
  readonly evidenceIds: readonly string[]
}
export interface SimulationDecisionBundle {
  readonly functionDecision: SimulationDecision
  readonly dataScopeDecision?: SimulationDecision
  readonly fieldPolicyDecision?: SimulationDecision
  readonly snapshotChecksum?: string
}
export interface AuthorizationSimulationView {
  readonly current: SimulationDecisionBundle
  readonly hypothetical: SimulationDecisionBundle
  readonly authVersion: number
  readonly policyVersion: number
  readonly snapshotChecksum: string
  readonly expiresAt: string
}
export interface AuthorizationSimulationCommand {
  readonly decisionRequest: {
      readonly subject: { readonly tenantId: string; readonly userId: string; readonly identitySub: string }
    readonly permissionCode: string
    readonly resource: { readonly applicationCode: string; readonly resourceCode: string }
    readonly requestedDecisions: readonly string[]
      readonly tokenVersions: { readonly authVersion: number; readonly policyVersion: number }
  }
  readonly hypothesis: { readonly addedPermissions: readonly string[]; readonly removedPermissions: readonly string[] }
  readonly at: string
}

export interface RoleChangeImpactView {
  readonly impact: {
    readonly roleId: string
    readonly activationRoots: readonly string[]
    readonly roleFamily: readonly string[]
    readonly effectiveFamilyRisk: string
    readonly permissionCount: number
    readonly conflicts: readonly string[]
  }
  readonly policyVersion: number
  readonly evidenceChecksum: string
  readonly expiresAt: string
}

export const simulationApi = (client: FeatureApiClient) => ({
  simulate: (command: AuthorizationSimulationCommand) => {
    const requestId = crypto.randomUUID()
    return client.request<AuthorizationSimulationView>('/api/rbac3/v1/simulations/authorization', {
      method: 'POST', body: command, headers: { 'X-Request-Id': requestId, 'X-Trace-Id': requestId },
    })
  },
  roleChangeImpact: (roleId: string) => {
    const requestId = crypto.randomUUID()
    return client.request<RoleChangeImpactView>('/api/rbac3/v1/simulations/role-change-impact', {
      method: 'POST',
      body: { roleId: roleId.trim(), at: new Date().toISOString() },
      headers: { 'X-Request-Id': requestId, 'X-Trace-Id': requestId },
    })
  },
})
