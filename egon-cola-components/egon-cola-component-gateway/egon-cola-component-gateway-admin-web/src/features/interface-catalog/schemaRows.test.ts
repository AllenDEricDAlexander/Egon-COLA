import { describe, expect, it } from 'vitest'
import { buildSchemaRows } from './schemaRows'

describe('buildSchemaRows', () => {
  it('expands nested protobuf objects and repeated fields', () => {
    const rows = buildSchemaRows({
      type: 'object',
      messageType: 'shop.v1.CreateOrderRequest',
      required: ['customerId'],
      properties: {
        customerId: {
          type: 'string',
          protobufType: 'STRING',
          protobufName: 'customer_id',
          fieldNumber: 1,
          description: '客户编号',
        },
        sku: {
          type: 'array',
          protobufType: 'STRING',
          protobufName: 'sku',
          fieldNumber: 2,
          description: '商品 SKU 列表',
          items: {
            type: 'string',
            protobufType: 'STRING',
          },
        },
        deliveryAddress: {
          type: 'object',
          messageType: 'shop.v1.Address',
          protobufType: 'MESSAGE',
          description: '配送地址',
          properties: {
            province: {
              type: 'string',
              protobufType: 'STRING',
              description: '配送省份',
            },
          },
        },
      },
    })

    expect(rows).toHaveLength(3)
    expect(rows[0]).toMatchObject({
      name: 'customerId',
      path: 'customerId',
      type: 'string',
      technicalType: 'STRING · customer_id · #1',
      required: true,
      description: '客户编号',
    })
    expect(rows[1]).toMatchObject({
      name: 'sku',
      type: 'array<string>',
      technicalType: 'STRING · sku · #2',
      required: false,
    })
    expect(rows[2].children?.[0]).toMatchObject({
      name: 'province',
      path: 'deliveryAddress.province',
      description: '配送省份',
    })
  })

  it('shows enum values and truncation as constraints', () => {
    const rows = buildSchemaRows({
      type: 'object',
      properties: {
        state: {
          type: 'string',
          enum: ['PENDING', 'COMPLETED'],
          default: 'PENDING',
          truncated: true,
        },
      },
    })

    expect(rows[0].constraints).toEqual([
      '可选值: PENDING, COMPLETED',
      '默认值: PENDING',
      '结构已截断',
    ])
  })

  it('keeps a scalar root visible', () => {
    expect(buildSchemaRows({ type: 'string', format: 'uuid' }))
      .toEqual([expect.objectContaining({
        name: '$',
        path: '$',
        type: 'string',
        technicalType: 'uuid',
      })])
  })
})
