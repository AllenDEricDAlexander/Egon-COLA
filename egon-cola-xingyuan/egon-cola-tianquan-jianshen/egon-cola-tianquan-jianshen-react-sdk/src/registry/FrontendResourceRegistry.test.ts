import {describe, expect, it} from 'vitest'
import type {Rbac3AboutView} from '../types'
import {FrontendResourceRegistry, type FrontendResourceDefinition} from './FrontendResourceRegistry'

const about = (
  resourceCodes: readonly string[],
  permissions: readonly string[] = [],
  fieldPolicies: Record<string, unknown> = {},
) => ({
  user: {subject: 'alice', tenantId: 'tenant-a', status: 'ACTIVE'},
  currentApplicationCode: 'rbac3-admin',
  activeRoles: [],
  permissions,
  resourceCodes,
  fieldPolicies,
  landingRouteCode: null,
  authVersion: 1,
  policyVersion: 1,
}) as unknown as Rbac3AboutView

const definitions: readonly FrontendResourceDefinition[] = [
  {kind: 'MENU', code: 'iam', name: 'IAM'},
  {kind: 'ROUTE', code: 'roles', name: 'Roles', parentCode: 'iam', path: '/roles', componentKey: 'roles',
    apiResourceCodes: ['roles.list'], suggestedPermissionCode: 'role:read'},
  {kind: 'ACTION', code: 'role.create', name: 'Create', routeCode: 'roles', resourceCode: 'roles',
    apiResourceCodes: ['roles.create'], suggestedPermissionCode: 'role:create'},
  {kind: 'FIELD', code: 'role.secret', name: 'Secret', resourceCode: 'roles', fieldCode: 'secret',
    suggestedPermissionCode: 'role:read'},
]

describe('FrontendResourceRegistry', () => {
  it('filters recursive menu descendants and actions by about resource codes', () => {
    const registry = new FrontendResourceRegistry(definitions)
    expect(registry.navigation(about(['iam', 'roles'], ['iam:read', 'role:read']))[0].children[0].code).toBe('roles')
    expect(registry.canAccessRoute('roles', about(['roles'], ['role:read']))).toBe(true)
    expect(registry.canAccessRoute('roles', about([], ['role:read']))).toBe(false)
  })

  it('fails closed for an unknown field and returns the about policy for a known field', () => {
    const registry = new FrontendResourceRegistry(definitions)
    expect(registry.getField('roles', 'unknown', about([]))).toEqual({level: 'NONE', maskingStrategy: null})
    expect(registry.getField('roles', 'secret', about([], [], {
      role: {
        resourceCode: 'roles',
        fields: {secret: {level: 'MASKED_READ', maskingStrategy: 'FULL'}},
      },
    }))).toEqual({level: 'MASKED_READ', maskingStrategy: 'FULL'})
  })

  it('serializes suggestions and API declarations without using suggestions for access', () => {
    const registry = new FrontendResourceRegistry(definitions)
    expect(registry.canAccessRoute('roles', about([], ['role:read']))).toBe(false)
    expect(registry.serializable()).toEqual(expect.arrayContaining([
      expect.objectContaining({code: 'roles', suggestedPermissionCode: 'role:read', apiResourceCodes: ['roles.list']}),
    ]))
  })

  it('rejects duplicate codes and cyclic parent graphs', () => {
    expect(() => new FrontendResourceRegistry([
      definitions[0], definitions[0],
    ])).toThrow(/duplicate/)
    expect(() => new FrontendResourceRegistry([
      {kind: 'MENU', code: 'a', name: 'A', parentCode: 'b'},
      {kind: 'MENU', code: 'b', name: 'B', parentCode: 'a'},
    ])).toThrow(/cyclic/)
  })
})
