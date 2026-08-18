import {describe, expect, it} from 'vitest'
import type {Rbac3AboutView} from '../types'
import {FrontendResourceRegistry, type FrontendResourceDefinition} from './FrontendResourceRegistry'

const about = (permissions: readonly string[], fieldPolicies: Record<string, unknown> = {}) => ({
  user: {subject: 'alice', tenantId: 'tenant-a', status: 'ACTIVE'},
  currentApplicationCode: 'rbac3-admin',
  activeRoles: [],
  permissions,
  fieldPolicies,
  landingRouteCode: null,
  authVersion: 1,
  policyVersion: 1,
}) as unknown as Rbac3AboutView

const definitions: readonly FrontendResourceDefinition[] = [
  {kind: 'MENU', code: 'iam', name: 'IAM', permission: 'iam:read'},
  {kind: 'ROUTE', code: 'roles', name: 'Roles', permission: 'role:read', parentCode: 'iam', path: '/roles', componentKey: 'roles'},
  {kind: 'ACTION', code: 'role.create', name: 'Create', permission: 'role:create', routeCode: 'roles', resourceCode: 'roles'},
  {kind: 'FIELD', code: 'role.secret', name: 'Secret', permission: 'role:read', resourceCode: 'roles', fieldCode: 'secret'},
]

describe('FrontendResourceRegistry', () => {
  it('filters recursive menu descendants and actions by about permissions', () => {
    const registry = new FrontendResourceRegistry(definitions)
    expect(registry.navigation(about(['iam:read', 'role:read']))[0].children[0].code).toBe('roles')
    expect(registry.canAccessRoute('roles', about(['role:read']))).toBe(true)
    expect(registry.canAccessRoute('roles', about([]))).toBe(false)
  })

  it('fails closed for an unknown field and returns the about policy for a known field', () => {
    const registry = new FrontendResourceRegistry(definitions)
    expect(registry.getField('roles', 'unknown', about([]))).toEqual({level: 'NONE', maskingStrategy: null})
    expect(registry.getField('roles', 'secret', about([], {
      role: {
        resourceCode: 'roles',
        fields: {secret: {level: 'MASKED_READ', maskingStrategy: 'FULL'}},
      },
    }))).toEqual({level: 'MASKED_READ', maskingStrategy: 'FULL'})
  })

  it('rejects duplicate codes and cyclic parent graphs', () => {
    expect(() => new FrontendResourceRegistry([
      definitions[0], definitions[0],
    ])).toThrow(/duplicate/)
    expect(() => new FrontendResourceRegistry([
      {kind: 'MENU', code: 'a', name: 'A', permission: 'a', parentCode: 'b'},
      {kind: 'MENU', code: 'b', name: 'B', permission: 'b', parentCode: 'a'},
    ])).toThrow(/cyclic/)
  })
})
