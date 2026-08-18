import type {Rbac3AboutView} from '../types'

export type FrontendResourceKind = 'MENU' | 'ROUTE' | 'ACTION' | 'FIELD'

export interface FrontendResourceDefinition {
  readonly kind: FrontendResourceKind
  readonly code: string
  readonly name: string
  readonly permission: string
  readonly parentCode?: string
  readonly routeCode?: string
  readonly resourceCode?: string
  readonly fieldCode?: string
  readonly jsonPath?: string
  readonly path?: string
  readonly componentKey?: string
  readonly hidden?: boolean
  readonly order?: number
}

export interface FrontendNavigationNode extends FrontendResourceDefinition {
  readonly children: readonly FrontendNavigationNode[]
}

export interface FieldAccessResult {
  readonly level: 'NONE' | 'MASKED_READ' | 'READ' | 'WRITE'
  readonly maskingStrategy: string | null
}

/** Local, checked-in MENU/ROUTE/ACTION/FIELD definitions used by browser guards and CI projection. */
export class FrontendResourceRegistry {
  readonly definitions: readonly FrontendResourceDefinition[]

  constructor(definitions: readonly FrontendResourceDefinition[]) {
    this.definitions = definitions.map(normalize)
    validateDefinitions(this.definitions)
  }

  navigation(about: Rbac3AboutView): readonly FrontendNavigationNode[] {
    const visible = new Map<string, FrontendNavigationNode>()
    const children = new Map<string | undefined, FrontendResourceDefinition[]>()
    for (const definition of this.definitions.filter((value) => value.kind === 'MENU' || value.kind === 'ROUTE')) {
      const parent = definition.parentCode
      children.set(parent, [...(children.get(parent) ?? []), definition])
    }
    const build = (definition: FrontendResourceDefinition): FrontendNavigationNode | null => {
      if (!about.permissions.includes(definition.permission)) return null
      const descendants = (children.get(definition.code) ?? [])
        .sort(compareOrder)
        .map(build)
        .filter((value): value is FrontendNavigationNode => value !== null)
      if (definition.kind === 'MENU' && descendants.length === 0) return null
      return {...definition, children: descendants}
    }
    for (const root of (children.get(undefined) ?? []).sort(compareOrder)) {
      const node = build(root)
      if (node !== null && (node.kind !== 'ROUTE' || node.hidden !== true)) visible.set(node.code, node)
    }
    return [...visible.values()]
  }

  canAccessRoute(code: string, about: Rbac3AboutView): boolean {
    const route = this.definitions.find((value) => value.kind === 'ROUTE' && value.code === code)
    return route !== undefined && about.permissions.includes(route.permission)
  }

  getField(resourceCode: string, fieldCode: string, about: Rbac3AboutView): FieldAccessResult {
    for (const policy of Object.values(about.fieldPolicies)) {
      if (policy.resourceCode !== resourceCode || !policy.fields[fieldCode]) continue
      return policy.fields[fieldCode]
    }
    return {level: 'NONE', maskingStrategy: null}
  }

  serializable(): readonly FrontendResourceDefinition[] {
    return this.definitions.map(({kind, code, name, permission, parentCode, routeCode, resourceCode,
      fieldCode, jsonPath, path, componentKey, hidden, order}) => ({
      kind, code, name, permission, parentCode, routeCode, resourceCode, fieldCode,
      jsonPath, path, componentKey, hidden, order,
    }))
  }
}

const normalize = (value: FrontendResourceDefinition): FrontendResourceDefinition => ({
  ...value,
  code: value.code.trim(),
  name: value.name.trim(),
  permission: value.permission.trim(),
})

const compareOrder = (left: FrontendResourceDefinition, right: FrontendResourceDefinition) =>
  (left.order ?? Number.MAX_SAFE_INTEGER) - (right.order ?? Number.MAX_SAFE_INTEGER)
    || left.code.localeCompare(right.code)

const validateDefinitions = (definitions: readonly FrontendResourceDefinition[]): void => {
  const codes = new Set<string>()
  for (const definition of definitions) {
    if (!definition.code || !definition.name || !definition.permission || codes.has(definition.code)) {
      throw new Error(`invalid or duplicate local RBAC3 resource code: ${definition.code}`)
    }
    codes.add(definition.code)
    if (definition.kind === 'ROUTE'
      && (!definition.path?.startsWith('/') || !definition.componentKey)) {
      throw new Error(`route ${definition.code} requires a local path and componentKey`)
    }
    if (definition.kind === 'ACTION' && (!definition.routeCode || !definition.resourceCode)) {
      throw new Error(`action ${definition.code} requires routeCode and resourceCode`)
    }
    if (definition.kind === 'FIELD' && (!definition.resourceCode || !definition.fieldCode)) {
      throw new Error(`field ${definition.code} requires resourceCode and fieldCode`)
    }
  }
  for (const definition of definitions) {
    const seen = new Set<string>()
    let parent = definition.parentCode
    while (parent) {
      if (seen.has(parent)) throw new Error(`cyclic local RBAC3 resource parent chain: ${definition.code}`)
      seen.add(parent)
      parent = definitions.find((value) => value.code === parent)?.parentCode
    }
  }
}
