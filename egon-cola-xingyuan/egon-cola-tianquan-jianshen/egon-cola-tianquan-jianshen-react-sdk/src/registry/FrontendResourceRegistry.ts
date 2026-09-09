import type {
  FieldAccessResult,
  FrontendNavigationNode,
  FrontendResourceDefinition,
  Rbac3AboutView,
} from '../types'

export type {
  FieldAccessResult,
  FrontendNavigationNode,
  FrontendResourceDefinition,
  FrontendResourceKind,
} from '../types'

/** Local, checked-in MENU/ROUTE/ACTION/FIELD definitions used by browser guards and CI projection. */
export class FrontendResourceRegistry {
  readonly definitions: readonly FrontendResourceDefinition[]

  constructor(definitions: readonly FrontendResourceDefinition[]) {
    this.definitions = definitions.map(normalize)
    validateDefinitions(this.definitions)
  }

  navigation(about: Rbac3AboutView): readonly FrontendNavigationNode[] {
    const children = new Map<string | undefined, FrontendResourceDefinition[]>()
    for (const definition of this.definitions.filter(
      (value) => value.kind === 'MENU' || value.kind === 'ROUTE',
    )) {
      const parent = definition.parentCode
      children.set(parent, [...(children.get(parent) ?? []), definition])
    }

    const build = (definition: FrontendResourceDefinition): FrontendNavigationNode | null => {
      if (definition.kind === 'ROUTE' && definition.hidden === true) return null
      const descendants = (children.get(definition.code) ?? [])
        .sort(compareOrder)
        .map(build)
        .filter((value): value is FrontendNavigationNode => value !== null)
      const resourceVisible = this.canAccessResource(definition.code, about)
      if (definition.kind === 'MENU' && !resourceVisible && descendants.length === 0) return null
      if (definition.kind === 'ROUTE' && !resourceVisible) return null
      return {...definition, children: descendants}
    }

    return (children.get(undefined) ?? [])
      .sort(compareOrder)
      .map(build)
      .filter((value): value is FrontendNavigationNode => value !== null)
  }

  canAccessResource(code: string, about: Rbac3AboutView): boolean {
    return typeof code === 'string'
      && code.length > 0
      && about.resourceCodes.includes(code)
  }

  canAccessRoute(code: string, about: Rbac3AboutView): boolean {
    const route = this.definitions.find((value) => value.kind === 'ROUTE' && value.code === code)
    return route !== undefined && this.canAccessResource(route.code, about)
  }

  getField(resourceCode: string, fieldCode: string, about: Rbac3AboutView): FieldAccessResult {
    for (const policy of Object.values(about.fieldPolicies)) {
      if (policy.resourceCode !== resourceCode || !policy.fields[fieldCode]) continue
      return policy.fields[fieldCode]
    }
    return {level: 'NONE', maskingStrategy: null}
  }

  serializable(): readonly FrontendResourceDefinition[] {
    return this.definitions.map((definition) => ({
      kind: definition.kind,
      code: definition.code,
      name: definition.name,
      suggestedPermissionCode: definition.suggestedPermissionCode ?? null,
      apiResourceCodes: definition.apiResourceCodes ?? [],
      parentCode: definition.parentCode,
      routeCode: definition.routeCode,
      resourceCode: definition.resourceCode,
      fieldCode: definition.fieldCode,
      jsonPath: definition.jsonPath,
      path: definition.path,
      componentKey: definition.componentKey,
      hidden: definition.hidden,
      order: definition.order,
    }))
  }
}

const normalize = (value: FrontendResourceDefinition): FrontendResourceDefinition => ({
  ...value,
  code: value.code.trim(),
  name: value.name.trim(),
  suggestedPermissionCode: value.suggestedPermissionCode?.trim() || null,
  apiResourceCodes: [...new Set((value.apiResourceCodes ?? [])
    .map((code) => code.trim())
    .filter(Boolean))].sort(),
})

const compareOrder = (left: FrontendResourceDefinition, right: FrontendResourceDefinition) =>
  (left.order ?? Number.MAX_SAFE_INTEGER) - (right.order ?? Number.MAX_SAFE_INTEGER)
    || left.code.localeCompare(right.code)

const validateDefinitions = (definitions: readonly FrontendResourceDefinition[]): void => {
  const codes = new Set<string>()
  for (const definition of definitions) {
    if (!definition.code || !definition.name || codes.has(definition.code)) {
      throw new Error(`invalid or duplicate local Tianquan-Jianshen resource code: ${definition.code}`)
    }
    codes.add(definition.code)
    if (definition.kind !== 'ROUTE' && definition.kind !== 'ACTION'
      && (definition.apiResourceCodes?.length ?? 0) > 0) {
      throw new Error(`only routes and actions may declare APIs: ${definition.code}`)
    }
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
      if (seen.has(parent)) throw new Error(`cyclic local Tianquan-Jianshen resource parent chain: ${definition.code}`)
      seen.add(parent)
      parent = definitions.find((value) => value.code === parent)?.parentCode
    }
  }
}
