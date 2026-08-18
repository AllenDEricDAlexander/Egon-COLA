import { createHash } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const defaultDefinitionsPath = resolve(packageRoot, 'src/app/resourceDefinitions.json')

export const loadDefinitions = async (definitionsPath = defaultDefinitionsPath) => {
  const text = await readFile(definitionsPath, 'utf8')
  const value = JSON.parse(text)
  if (!Array.isArray(value)) throw new Error('resource definitions must be an array')
  return value
}

export const projectReport = (definitions, buildId, expectedApplicationVersion) => {
  const resources = definitions
    .filter((definition) => definition.kind !== 'FIELD')
    .map((definition) => ({
      type: definition.kind,
      code: definition.code,
      name: definition.name,
      parentCode: definition.parentCode ?? null,
      permissionCode: definition.permission ?? null,
      path: definition.path ?? null,
      componentKey: definition.componentKey ?? null,
      routeCode: definition.routeCode ?? null,
      order: definition.order ?? null,
      hidden: definition.hidden === true,
    }))
  const fields = definitions
    .filter((definition) => definition.kind === 'FIELD')
    .map((definition) => ({
      resourceCode: definition.resourceCode,
      fieldCode: definition.fieldCode,
      jsonPath: definition.jsonPath,
      dataType: definition.dataType ?? 'STRING',
    }))
  const request = { buildId, checksum: '', expectedApplicationVersion, resources, fields }
  request.checksum = canonicalChecksum(request)
  return request
}

export const canonicalChecksum = (request) => {
  const resources = [...request.resources]
    .sort((left, right) => `${left.type}:${left.code}`.localeCompare(`${right.type}:${right.code}`))
    .map((value) => [
      value.type,
      value.code,
      value.name,
      value.parentCode ?? '',
      value.permissionCode ?? '',
      value.path ?? '',
      value.componentKey ?? '',
      value.routeCode ?? '',
      value.order === null || value.order === undefined ? 'null' : String(value.order),
      String(value.hidden),
    ].join('|')).join(';')
  const fields = [...request.fields]
    .sort((left, right) => `${left.resourceCode}:${left.fieldCode}`.localeCompare(`${right.resourceCode}:${right.fieldCode}`))
    .map((value) => [value.resourceCode, value.fieldCode, value.jsonPath, value.dataType].join('|')).join(';')
  const canonical = `${request.buildId}|resources=${resources};|fields=${fields};`
  return `sha256:${createHash('sha256').update(canonical, 'utf8').digest('hex')}`
}

export const reportResources = async ({
  baseUrl,
  businessCode,
  applicationCode,
  serviceAccessToken,
  buildId,
  expectedApplicationVersion,
  definitionsPath = defaultDefinitionsPath,
  fetcher = globalThis.fetch,
}) => {
  const required = { baseUrl, businessCode, applicationCode, serviceAccessToken, buildId }
  for (const [name, value] of Object.entries(required)) {
    if (typeof value !== 'string' || value.trim() === '') throw new Error(`${name} is required`)
  }
  if (!Number.isInteger(expectedApplicationVersion) || expectedApplicationVersion < 0) {
    throw new Error('expectedApplicationVersion must be a non-negative integer')
  }
  const request = projectReport(await loadDefinitions(definitionsPath), buildId.trim(), expectedApplicationVersion)
  const endpoint = `${baseUrl.replace(/\/$/, '')}/api/rbac3/v1/iam/resource-catalog/businesses/${encodeURIComponent(businessCode.trim())}/applications/${encodeURIComponent(applicationCode.trim())}/frontend-resources`
  const response = await fetcher(endpoint, {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${serviceAccessToken}`,
    },
    body: JSON.stringify(request),
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok) throw new Error(`resource report failed with HTTP ${response.status}`)
  if (payload?.success === false) throw new Error(payload.message ?? 'resource report rejected')
  return payload?.data ?? payload
}

const run = async () => {
  const result = await reportResources({
    baseUrl: process.env.RBAC3_GATEWAY_BASE_URL,
    businessCode: process.env.RBAC3_BUSINESS_CODE,
    applicationCode: process.env.RBAC3_APPLICATION_CODE,
    serviceAccessToken: process.env.RBAC3_SERVICE_ACCESS_TOKEN,
    buildId: process.env.RBAC3_BUILD_ID,
    expectedApplicationVersion: Number(process.env.RBAC3_EXPECTED_APPLICATION_VERSION ?? 0),
  })
  process.stdout.write(`${JSON.stringify(result)}\n`)
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url))) {
  run().catch((error) => {
    process.stderr.write(`${error instanceof Error ? error.message : 'resource report failed'}\n`)
    process.exitCode = 1
  })
}
