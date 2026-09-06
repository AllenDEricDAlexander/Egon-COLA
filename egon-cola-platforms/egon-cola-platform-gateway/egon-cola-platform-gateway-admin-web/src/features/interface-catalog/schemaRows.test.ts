import { describe, expect, it } from 'vitest'
import { buildSchemaRows } from './schemaRows'

describe('buildSchemaRows', () => {
  it('renders OpenAPI 3.1 nullable types and boolean schemas', () => {
    const rows = buildSchemaRows({type: 'object', properties: {
      name: {type: ['string', 'null'], description: '可空名称'},
      items: {type: ['array', 'null'], items: {type: 'integer'}},
      anyValue: true,
      forbidden: false,
      referencedAny: {$ref: '#/$defs/Any'},
    }, $defs: {Any: true}})
    expect(rows[0]).toMatchObject({type: 'string | null', description: '可空名称'})
    expect(rows[0].constraints).toContain('允许 null')
    expect(rows[1].type).toBe('array<integer> | null')
    expect(rows[2].type).toBe('any')
    expect(rows[3].type).toBe('never')
    expect(rows[4]).toMatchObject({type: 'any', technicalType: '#/$defs/Any'})
  })

  it('shows composition branches without merging conditional required fields', () => {
    const rows = buildSchemaRows({allOf: [
      {$ref: '#/$defs/Base'},
      {type: 'object', properties: {choice: {oneOf: [{type: 'string'}, {type: 'integer'}]}}},
    ], $defs: {Base: {type: 'object', required: ['id'], properties: {id: {type: 'string', description: '标识'}}}}})
    expect(rows[0].type).toBe('allOf')
    expect(rows[0].children?.map((row) => row.name)).toEqual(['allOf[1]', 'allOf[2]'])
    expect(rows[0].children?.[0].children?.[0]).toMatchObject({name: 'id', required: true, description: '标识'})
    const choice = rows[0].children?.[1].children?.[0]
    expect(choice?.type).toBe('oneOf')
    expect(choice?.children?.map((row) => row.type)).toEqual(['string', 'integer'])
  })
  it('expands local OpenAPI definitions at the root and inside arrays', () => {
    const rows = buildSchemaRows({
      $ref: '#/$defs/Result',
      $defs: {
        Result: {type: 'object', required: ['data'], properties: {
          data: {type: 'array', items: {$ref: '#/$defs/Item'}},
        }},
        Item: {type: 'object', properties: {id: {type: 'string', description: '资源标识'}}},
      },
    })
    expect(rows[0]).toMatchObject({name: 'data', type: 'array<object>', required: true})
    expect(rows[0].children?.[0]).toMatchObject({name: 'id', description: '资源标识'})
  })

  it('handles escaped pointers and resolves shared definitions on independent branches', () => {
    const rows = buildSchemaRows({type: 'object', properties: {
      left: {$ref: '#/$defs/A~1B~0'}, right: {$ref: '#/$defs/A~1B~0'},
    }, $defs: {'A/B~': {type: 'object', properties: {name: {type: 'string'}}}}})
    expect(rows.map((row) => row.children?.[0].path)).toEqual(['left.name', 'right.name'])
  })

  it('keeps recursive and external references visible without unbounded expansion', () => {
    const rows = buildSchemaRows({$ref: '#/$defs/Node', $defs: {
      Node: {type: 'object', properties: {children: {type: 'array', items: {$ref: '#/$defs/Node'}}}},
    }})
    expect(rows[0].type).toBe('array<object>')
    expect(rows[0].constraints).toContain('递归引用，已停止展开')
    expect(rows[0].children).toBeUndefined()
    expect(buildSchemaRows({$ref: 'https://example.invalid/schema'})[0].technicalType)
      .toBe('https://example.invalid/schema')
  })

  it('bounds wide and deeply nested schemas', () => {
    const rows = buildSchemaRows({type: 'object', properties: Object.fromEntries(
      Array.from({length: 1500}, (_, index) => [`field${index}`, {type: 'string'}]),
    )})
    expect(rows).toHaveLength(1000)
    expect(rows.at(-1)?.constraints).toContain('字段数量达到显示上限')
    let schema: Record<string, unknown> = {type: 'string'}
    for (let depth = 0; depth < 30; depth++) schema = {type: 'object', properties: {next: schema}}
    let last = buildSchemaRows(schema)[0]
    let depth = 1
    while (last.children?.length) {
      last = last.children[0]
      depth++
    }
    expect(depth).toBeLessThanOrEqual(17)
    expect(last.constraints).toContain('结构达到显示上限')
  })
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
