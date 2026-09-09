export type SchemaRow = {
  key: string
  name: string
  path: string
  type: string
  technicalType: string
  required: boolean
  description?: string
  constraints: string[]
  children?: SchemaRow[]
}

type SchemaNode = Record<string, unknown>

type SchemaContext = {
  root: SchemaNode
  references: ReadonlySet<string>
  depth: number
  budget: {remaining: number}
}

const MAX_DEPTH = 16
const MAX_ROWS = 1000

// Resolve document-local references only; schemas never trigger network requests.
const resolveReferences = (schema: SchemaNode, context: SchemaContext) => {
  let node = schema
  const references = new Set(context.references)
  let recursive = false
  while (typeof node.$ref === 'string' && (node.$ref === '#' || node.$ref.startsWith('#/'))) {
    const reference = node.$ref
    let target: unknown = context.root
    try {
      for (const part of decodeURIComponent(reference.slice(2)).split('/').filter(() => reference !== '#')) {
        const key = part.replace(/~1/g, '/').replace(/~0/g, '~')
        target = typeof target === 'object' && target !== null && Object.hasOwn(target, key)
          ? (target as SchemaNode)[key] : undefined
      }
    } catch {
      break
    }
    const resolved = schemaNode(target)
    if (!resolved) break
    const {$ref: ignoredReference, ...siblings} = node
    void ignoredReference
    node = {...resolved, ...siblings}
    if (references.has(reference)) {
      recursive = true
      break
    }
    references.add(reference)
  }
  return {
    node,
    references,
    recursive,
  }
}

export const buildSchemaRows = (schema: SchemaNode): SchemaRow[] => {
  if (Object.keys(schema).length === 0) return []
  const context: SchemaContext = {root: schema, references: new Set(), depth: 0, budget: {remaining: MAX_ROWS}}
  const resolved = resolveReferences(schema, context)
  const properties = record(resolved.node.properties)
  if (properties && !resolved.recursive) {
    const resolvedContext = {...context, references: resolved.references}
    return [
      ...propertyRows(properties, '', requiredNames(resolved.node), resolvedContext),
      ...compositionRows(resolved.node, '', resolvedContext),
    ]
  }
  return [schemaRow('$', '$', schema, false, context)]
}

const propertyRows = (
  properties: SchemaNode,
  parentPath: string,
  required: Set<string>,
  context: SchemaContext,
): SchemaRow[] => {
  const rows: SchemaRow[] = []
  for (const [name, value] of Object.entries(properties)) {
    const node = schemaNode(value)
    if (!node) continue
    if (context.budget.remaining <= 0) {
      rows.at(-1)?.constraints.push('字段数量达到显示上限')
      break
    }
    const path = parentPath ? `${parentPath}.${name}` : name
    rows.push(schemaRow(name, path, node, required.has(name), context))
  }
  return rows
}

const schemaRow = (
  name: string,
  path: string,
  schema: SchemaNode,
  required: boolean,
  context: SchemaContext,
): SchemaRow => {
  context.budget.remaining--
  const originalReference = schema.$ref
  const resolved = resolveReferences(schema, context)
  schema = resolved.node
  const childContext = {...context, references: resolved.references, depth: context.depth + 1}
  const row: SchemaRow = {
    key: path,
    name,
    path,
    type: displayType(schema, childContext),
    technicalType: technicalType(originalReference ? {...schema, $ref: originalReference} : schema),
    required,
    description: stringValue(schema.description),
    constraints: constraints(schema),
  }
  const isArray = schema.type === 'array' || (Array.isArray(schema.type) && schema.type.includes('array'))
  const nested = isArray
    ? resolveReferences(schemaNode(schema.items) ?? {}, childContext) : resolved
  if (resolved.recursive || nested.recursive) {
    row.constraints.push('递归引用，已停止展开')
    return row
  }
  if (context.depth >= MAX_DEPTH || context.budget.remaining <= 0) {
    row.constraints.push('结构达到显示上限')
    return row
  }
  childContext.references = nested.references
  const properties = record(nested.node.properties)
  if (properties) {
    row.children = propertyRows(
      properties,
      path === '$' ? '' : path,
      requiredNames(nested.node),
      childContext,
    )
  } else {
    const additionalProperties = record(nested.node.additionalProperties)
    if (additionalProperties) {
      const childPath = path === '$' ? '{value}' : `${path}.{value}`
      row.children = [schemaRow(
        '{value}',
        childPath,
        additionalProperties,
        false,
        childContext,
      )]
    }
  }
  const composed = compositionRows(nested.node, path === '$' ? '' : path, childContext)
  if (composed.length > 0) row.children = [...(row.children ?? []), ...composed]
  return row
}

const compositionRows = (schema: SchemaNode, parentPath: string, context: SchemaContext): SchemaRow[] => {
  const rows: SchemaRow[] = []
  for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
    const branches = schema[keyword]
    if (!Array.isArray(branches)) continue
    for (const [index, value] of branches.entries()) {
      if (context.budget.remaining <= 0) break
      const node = schemaNode(value)
      if (!node) continue
      const name = `${keyword}[${index + 1}]`
      const path = parentPath ? `${parentPath}.${name}` : name
      rows.push(schemaRow(name, path, node, false, context))
    }
  }
  return rows
}

const displayType = (schema: SchemaNode, context: SchemaContext): string => {
  const types = Array.isArray(schema.type) ? schema.type.filter((type) => typeof type === 'string') : [schemaType(schema)]
  return types.map((type) => {
    if (type === 'array') {
      return `array<${schemaType(resolveReferences(schemaNode(schema.items) ?? {}, context).node)}>`
    }
    if (type === 'object' && record(schema.additionalProperties)) {
      return `object<${schemaType(resolveReferences(record(schema.additionalProperties) ?? {}, context).node)}>`
    }
    return type
  }).join(' | ')
}

const schemaType = (schema: SchemaNode): string => {
  if (record(schema.not) && Object.keys(schema.not as SchemaNode).length === 0) return 'never'
  if (Array.isArray(schema.type)) return schema.type.filter((type) => typeof type === 'string').join(' | ')
  const type = stringValue(schema.type)
  if (type) return type
  if (record(schema.properties)) return 'object'
  const composition = ['allOf', 'anyOf', 'oneOf'].filter((keyword) => Array.isArray(schema[keyword]))
  if (composition.length > 0) return composition.join(' / ')
  return schema.$ref ? 'unknown' : 'any'
}

const technicalType = (schema: SchemaNode): string => {
  const values = [
    stringValue(schema.format),
    stringValue(schema.javaType),
    stringValue(schema.protobufType),
    stringValue(schema.messageType),
    stringValue(schema.enumType),
    stringValue(schema.protobufName),
    typeof schema.fieldNumber === 'number' ? `#${schema.fieldNumber}` : undefined,
    stringValue(schema.$ref),
  ].filter((value): value is string => Boolean(value))
  return [...new Set(values)].join(' · ')
}

const constraints = (schema: SchemaNode): string[] => {
  const result: string[] = []
  if (Array.isArray(schema.enum)) {
    result.push(`可选值: ${schema.enum.map(displayValue).join(', ')}`)
  }
  addConstraint(result, '默认值', schema.default)
  addConstraint(result, '最小值', schema.minimum)
  addConstraint(result, '最大值', schema.maximum)
  addConstraint(result, '最小长度', schema.minLength)
  addConstraint(result, '最大长度', schema.maxLength)
  addConstraint(result, '格式', schema.pattern)
  if (schema.nullable === true || (Array.isArray(schema.type) && schema.type.includes('null'))) result.push('允许 null')
  if (Array.isArray(schema.allOf)) result.push('同时满足全部 allOf 分支')
  if (Array.isArray(schema.anyOf)) result.push('至少满足一个 anyOf 分支')
  if (Array.isArray(schema.oneOf)) result.push('恰好满足一个 oneOf 分支')
  if (schemaType(schema) === 'never') result.push('不接受任何值')
  if (schema.truncated === true) result.push('结构已截断')
  return result
}

const addConstraint = (
  target: string[],
  label: string,
  value: unknown,
) => {
  if (value !== undefined && value !== null && value !== '') {
    target.push(`${label}: ${displayValue(value)}`)
  }
}

const requiredNames = (schema: SchemaNode): Set<string> => new Set(
  Array.isArray(schema.required)
    ? schema.required.filter((value): value is string => typeof value === 'string')
    : [],
)

const displayValue = (value: unknown): string => {
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

const stringValue = (value: unknown): string | undefined => (
  typeof value === 'string' && value.length > 0 ? value : undefined
)

const record = (value: unknown): SchemaNode | undefined => (
  typeof value === 'object' && value !== null && !Array.isArray(value)
    ? value as SchemaNode
    : undefined
)

const schemaNode = (value: unknown): SchemaNode | undefined => (
  typeof value === 'boolean' ? (value ? {} : {not: {}}) : record(value)
)
