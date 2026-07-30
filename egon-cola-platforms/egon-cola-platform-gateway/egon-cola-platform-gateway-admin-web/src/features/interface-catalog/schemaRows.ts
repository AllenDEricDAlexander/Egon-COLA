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

export const buildSchemaRows = (schema: SchemaNode): SchemaRow[] => {
  if (Object.keys(schema).length === 0) return []
  const properties = record(schema.properties)
  if (properties) {
    return propertyRows(properties, '', requiredNames(schema))
  }
  return [schemaRow('$', '$', schema, false)]
}

const propertyRows = (
  properties: SchemaNode,
  parentPath: string,
  required: Set<string>,
): SchemaRow[] => Object.entries(properties)
  .filter((entry): entry is [string, SchemaNode] => record(entry[1]) !== undefined)
  .map(([name, value]) => {
    const path = parentPath ? `${parentPath}.${name}` : name
    return schemaRow(name, path, value, required.has(name))
  })

const schemaRow = (
  name: string,
  path: string,
  schema: SchemaNode,
  required: boolean,
): SchemaRow => {
  const row: SchemaRow = {
    key: path,
    name,
    path,
    type: displayType(schema),
    technicalType: technicalType(schema),
    required,
    description: stringValue(schema.description),
    constraints: constraints(schema),
  }
  const nested = nestedSchema(schema)
  const properties = record(nested.properties)
  if (properties) {
    row.children = propertyRows(
      properties,
      path === '$' ? '' : path,
      requiredNames(nested),
    )
  } else {
    const additionalProperties = record(nested.additionalProperties)
    if (additionalProperties) {
      const childPath = path === '$' ? '{value}' : `${path}.{value}`
      row.children = [schemaRow(
        '{value}',
        childPath,
        additionalProperties,
        false,
      )]
    }
  }
  return row
}

const nestedSchema = (schema: SchemaNode): SchemaNode => {
  if (schema.type === 'array') return record(schema.items) ?? {}
  return schema
}

const displayType = (schema: SchemaNode): string => {
  const type = schemaType(schema)
  if (type === 'array') {
    return `array<${schemaType(record(schema.items) ?? {})}>`
  }
  if (type === 'object' && record(schema.additionalProperties)) {
    return `object<${schemaType(record(schema.additionalProperties) ?? {})}>`
  }
  return type
}

const schemaType = (schema: SchemaNode): string => {
  const type = stringValue(schema.type)
  if (type) return type
  if (record(schema.properties)) return 'object'
  return 'unknown'
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
  if (schema.nullable === true) result.push('允许 null')
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
