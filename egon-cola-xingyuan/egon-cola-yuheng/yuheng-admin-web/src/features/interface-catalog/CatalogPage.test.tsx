import { describe, expect, it } from 'vitest'
import type { CatalogTree } from '../../api/types'
import { toTree } from './CatalogPage'

const catalog: CatalogTree = {
  applicationId: 'application-1',
  businessDomains: [{
    id: 'business-1',
    code: 'retail',
    displayName: 'Retail',
    entityDomains: [{
      id: 'entity-1',
      code: 'orders',
      displayName: 'Orders',
      interfaceGroups: [
        {
          id: 'group-http',
          code: 'http',
          displayName: 'HTTP',
          sourceType: 'OPENAPI31',
          operations: [{
            id: 'operation-http',
            operationKey: 'orders.get',
            protocol: 'HTTP',
            methodIdentity: 'GET /orders',
            externalAccessible: true,
            lifecycleStatus: 'ACTIVE',
            sourceType: 'OPENAPI31',
            revision: 1,
          }],
        },
        {
          id: 'group-rpc',
          code: 'rpc',
          displayName: 'RPC',
          sourceType: 'RPC_DESCRIPTOR',
          operations: [{
            id: 'operation-rpc',
            operationKey: 'orders.lookup',
            protocol: 'RPC',
            methodIdentity: 'Lookup',
            externalAccessible: false,
            lifecycleStatus: 'ACTIVE',
            sourceType: 'RPC_DESCRIPTOR',
            revision: 1,
          }],
        },
      ],
    }],
  }],
}

describe('CatalogPage source filter', () => {
  it('keeps only the selected source group and its operation branch', () => {
    const tree = toTree(catalog, '', 'OPENAPI31')

    expect(tree).toHaveLength(1)
    expect(tree[0].children).toHaveLength(1)
    expect(tree[0].children?.[0].children).toHaveLength(1)
    expect(tree[0].children?.[0].children?.[0].key).toBe('g:group-http')
  })

  it('supports searching while retaining the complete hierarchy for matches', () => {
    const tree = toTree(catalog, 'lookup', 'ALL')

    expect(tree[0].children?.[0].children?.[0].key).toBe('g:group-rpc')
    expect(tree[0].children?.[0].children?.[0].children?.[0].key)
      .toBe('o:operation-rpc')
  })
})
