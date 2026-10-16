import { test as base, expect, type Page } from '@playwright/test'
import { createHash, createPublicKey, randomBytes, randomUUID, verify as verifySignature } from 'node:crypto'
import { createServer } from 'node:https'
import { closeSync, existsSync, fsyncSync, linkSync, openSync, renameSync, rmSync, unlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { bffRefreshWaitMilliseconds, canonicalSessionProof, type CanonicalSessionProof, loadIntegrationInputs, privateDirectory, readPrivateFile, readIntegrationJson, taskControl, type IntegrationInputs, type IntegrationClientFact, type IntegrationPublicClientFact } from '../../playwright.integration.config'

const test = base.extend({
  context: async ({ playwright }, use) => {
    const inputs = loadIntegrationInputs()
    const profile = join(inputs.root, 'browser-profiles', randomUUID())
    privateDirectory(profile, inputs.root)
    const context = await playwright.chromium.launchPersistentContext(profile, {
      baseURL: inputs.origin,
      headless: true,
      ignoreHTTPSErrors: false,
      args: [`--ignore-certificate-errors-spki-list=${inputs.certificatePin}`],
    })
    try {
      await use(context)
    } finally {
      await context.close()
      rmSync(profile, { recursive: true })
    }
  },
})

test.afterEach(async ({}, info) => {
  if (!info.title.startsWith('LIVE-')) return
  const inputs = loadIntegrationInputs()
  marker(inputs, 'BROWSER_EXECUTION', info.status === 'passed' && info.expectedStatus === 'passed' ? 'PASSED' : 'FAILED', {
    titleReference: createHash('sha256').update(info.title).digest('hex'),
    expectedStatus: info.expectedStatus, actualStatus: info.status, retry: info.retry, executedTestCount: 1,
  })
})

type Realm = 'default' | 'tenant-b'
const children = [
  { key: 'tianquan-shoubing', alias: 'edge-shoubing', route: '/xingyuan/tianquan-shoubing/overview',
    bootstrap: '/api/v1/tianquan-shoubing/auth/bootstrap', list: '/api/v1/tianquan-shoubing/clients' },
  { key: 'tianquan-jianshen', alias: 'edge-jianshen', route: '/xingyuan/tianquan-jianshen/roles',
    bootstrap: '/api/v1/auth/about', list: '/api/tianquan-jianshen/v1/iam/roles' },
  { key: 'yuheng', alias: 'edge-yuheng', route: '/xingyuan/yuheng/dashboard',
    bootstrap: '/api/v1/auth/bootstrap', list: '/api/v1/yuheng/admin/scopes' },
  { key: 'tianshu', alias: 'edge-tianshu', route: '/xingyuan/tianshu/registry',
    bootstrap: '/api/v1/tianshu/auth/bootstrap', list: '/api/v1/tianshu/registry/services/page?pageNo=1&pageSize=10' },
] as const

function client(inputs: IntegrationInputs, realm: Realm, alias: string): IntegrationClientFact {
  const values = inputs.clients.filter(value => value.realmCode === realm && value.alias === alias)
  if (values.length !== 1) throw new Error('XINGYUAN_TARGET_UNSAFE')
  return values[0]
}

function marker(inputs: IntegrationInputs, name: string, status: string, details: Record<string, unknown> = {}): void {
  if (!/^[A-Z0-9_-]{1,80}$/.test(name)) throw new Error('XINGYUAN_INPUT_INVALID')
  const directory = inputs.evidenceDirectory
  const target = join(directory, `browser-${name}.json`)
  const temporary = join(directory, `.browser-${randomUUID()}.tmp`)
  try {
    writeFileSync(temporary, JSON.stringify({ schemaVersion: 1, runId: inputs.runId, manifestSha256: inputs.manifestSha256,
      status, completedAt: new Date().toISOString(), ...details }), { flag: 'wx', mode: 0o600 })
    const frame = openSync(temporary, 'r')
    try { fsyncSync(frame) } finally { closeSync(frame) }
    linkSync(temporary, target)
    unlinkSync(temporary)
    const parent = openSync(directory, 'r')
    try { fsyncSync(parent) } finally { closeSync(parent) }
  } finally {
    if (existsSync(temporary)) unlinkSync(temporary)
  }
}

async function ownerReceipt(inputs: IntegrationInputs, name: string, status: string): Promise<void> {
  const path = join(inputs.evidenceDirectory, `oracle-${name}.json`)
  await expect.poll(() => {
    if (!existsSync(path)) return 'WAITING'
    const value: unknown = readIntegrationJson(readPrivateFile(path, inputs.evidenceDirectory))
    if (!value || typeof value !== 'object') throw new Error('XINGYUAN_TARGET_UNSAFE')
    const receipt = value as Record<string, unknown>
    if (receipt.schemaVersion !== 1 || receipt.runId !== inputs.runId || receipt.manifestSha256 !== inputs.manifestSha256) {
      throw new Error('XINGYUAN_TARGET_UNSAFE')
    }
    if (receipt.status !== status && receipt.status !== 'WAITING') throw new Error('XINGYUAN_VERIFICATION_FAILED')
    if (receipt.status === status) {
      if (receipt.phase !== name || typeof receipt.recordedAt !== 'string' || !receipt.recordedAt.endsWith('Z')
          || !Number.isFinite(Date.parse(receipt.recordedAt)) || !Array.isArray(receipt.proofs)) throw new Error('XINGYUAN_VERIFICATION_FAILED')
      const realm: Realm = name.startsWith('TENANT_A_') ? 'default' : 'tenant-b'
      const expectedAliases = name.endsWith('_REFRESH') ? children.map(value => value.alias)
        : name.endsWith('_STEP_UP') ? ['edge-ui'] : ['edge-ui', ...children.map(value => value.alias)]
      if (receipt.proofs.length !== expectedAliases.length) throw new Error('XINGYUAN_VERIFICATION_FAILED')
      const observed = new Set<string>()
      for (const item of receipt.proofs) {
        if (!item || typeof item !== 'object') throw new Error('XINGYUAN_VERIFICATION_FAILED')
        const proof = item as Record<string, unknown>
        const proofFields = ['alias', 'issuer', 'registrationId', 'tenantId', 'resourceUri', 'kind', 'authorizationReference',
          'tokenProofReference', 'eventProofReference', 'issuedAt', 'expiresAt', 'authenticationTime', 'beforeStatus', 'afterStatus']
        if (Object.keys(proof).length !== proofFields.length || Object.keys(proof).some(key => !proofFields.includes(key))) {
          throw new Error('XINGYUAN_VERIFICATION_FAILED')
        }
        if (typeof proof.alias !== 'string' || !expectedAliases.includes(proof.alias) || observed.has(proof.alias)) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        observed.add(proof.alias)
        const target = client(inputs, realm, proof.alias)
        if (proof.issuer !== target.issuer || proof.registrationId !== target.registrationId || proof.tenantId !== inputs.tenantIds[realm]
            || proof.resourceUri !== target.resourceUris[0] || proof.kind !== (proof.alias === 'edge-ui' ? 'IDENTITY' : 'RESOURCE')
            || ['authorizationReference', 'tokenProofReference', 'eventProofReference'].some(field => typeof proof[field] !== 'string' || !/^[0-9a-f]{64}$/.test(proof[field] as string))
            || typeof proof.issuedAt !== 'string' || typeof proof.expiresAt !== 'string' || !proof.issuedAt.endsWith('Z') || !proof.expiresAt.endsWith('Z')
            || !Number.isFinite(Date.parse(proof.issuedAt)) || !Number.isFinite(Date.parse(proof.expiresAt))
            || Date.parse(proof.expiresAt) <= Date.parse(receipt.recordedAt)) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        if ((name.endsWith('_HOLD') || name.endsWith('_REPLAY')) && proof.beforeStatus !== 200) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        if (name.endsWith('_REPLAY') && proof.afterStatus !== 401) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        if (name.endsWith('_STEP_UP') && (typeof proof.authenticationTime !== 'string' || !proof.authenticationTime.endsWith('Z')
          || !Number.isFinite(Date.parse(proof.authenticationTime)))) throw new Error('XINGYUAN_VERIFICATION_FAILED')
      }
    }
    return receipt.status
  }, { timeout: inputs.timeoutMs, intervals: [100, 250, 500] }).toBe(status)
}

async function status(page: Page, path: string, method = 'GET'): Promise<number> {
  return page.evaluate(async ({ target, verb }) => {
    const response = await fetch(target, { method: verb, credentials: 'include', redirect: 'manual', cache: 'no-store' })
    return response.status
  }, { target: path, verb: method })
}

async function sessionMatches(page: Page, inputs: IntegrationInputs, realm: Realm): Promise<void> {
  const valid = await page.evaluate(async expected => {
    const response = await fetch('/bff/session', { credentials: 'include', cache: 'no-store' })
    if (response.status !== 200 || !response.headers.get('cache-control')?.includes('no-store')) return false
    const session = await response.json() as { authenticated?: boolean; tenantId?: string; subject?: string }
    return session.authenticated === true && session.tenantId === expected.tenant && session.subject === expected.subject
  }, { tenant: inputs.tenantIds[realm], subject: inputs.subjects.admin })
  expect(valid).toBe(true)
}

/** Complete only actual AS forms. Failure reporting deliberately excludes password/callback/code call logs. */
async function authorize(page: Page, inputs: IntegrationInputs, realm: Realm, alias: string,
                         returnTo: string, explicit = false, stepUp = false): Promise<boolean> {
  const expected = client(inputs, realm, alias)
  const selector = explicit ? expected.registrationId : alias
  const query = new URLSearchParams({ returnTo })
  if (stepUp) query.set('stepUp', 'true')
  let verifiedRequest = false
  const observe = (request: import('@playwright/test').Request) => {
    const url = new URL(request.url())
    if (url.origin !== inputs.origin || url.pathname !== `/tenants/${inputs.tenantIds[realm]}/oauth2/authorize`
        || url.searchParams.get('client_id') !== expected.clientId) return
    verifiedRequest = url.searchParams.get('code_challenge_method') === 'S256'
      && /^[A-Za-z0-9_-]{43}$/.test(url.searchParams.get('code_challenge') ?? '')
      && Boolean(url.searchParams.get('state')) && Boolean(url.searchParams.get('nonce'))
      && url.searchParams.get('resource') === expected.resourceUris[0]
      && (!stepUp || (url.searchParams.get('prompt') === 'login' && url.searchParams.get('max_age') === '0'))
  }
  page.on('request', observe)
  let passwordPrompt = false
  try {
    await page.goto(`/oauth2/authorization/${encodeURIComponent(selector)}?${query}`)
    for (let stage = 0; stage < 5; stage++) {
      const current = new URL(page.url())
      if (current.origin !== inputs.origin) throw new Error('XINGYUAN_TARGET_UNSAFE')
      if (current.pathname === `/tenants/${inputs.tenantIds[realm]}/login`) {
        if (passwordPrompt) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        passwordPrompt = true
        await page.getByLabel('Username', { exact: true }).fill('alice')
        const file = inputs.secretFiles['admin-password']
        if (!file) throw new Error('XINGYUAN_INPUT_UNAVAILABLE')
        const password = readPrivateFile(file, join(inputs.root, 'secrets'), 4096)
        try {
          await page.getByLabel('Password', { exact: true }).fill(password.toString('utf8').replace(/\r?\n$/, ''))
          await page.getByRole('button', { name: 'Sign in', exact: true }).click()
        } finally {
          password.fill(0)
        }
      } else if (current.pathname === `/tenants/${inputs.tenantIds[realm]}/consent`) {
        const requested = await page.locator('input[type="checkbox"][name="scope"]').evaluateAll(elements =>
          elements.map(value => (value as HTMLInputElement).value).sort())
        expect(requested.every(value => expected.allowedScopes.includes(value))).toBe(true)
        expect(requested.length > 0).toBe(true)
        for (const checkbox of await page.locator('input[type="checkbox"][name="scope"]').all()) await checkbox.check()
        await page.getByRole('button', { name: 'Allow selected permissions', exact: true }).click()
      } else if (!current.pathname.startsWith('/login/oauth2/') && !current.pathname.startsWith('/oauth2/')) {
        break
      } else {
        await page.waitForTimeout(100)
      }
    }
    expect(verifiedRequest).toBe(true)
    if (stepUp) expect(passwordPrompt).toBe(true)
    await sessionMatches(page, inputs, realm)
    return passwordPrompt
  } finally {
    page.off('request', observe)
  }
}

async function captureCanonicalSession(page: Page, inputs: IntegrationInputs, realm: Realm): Promise<{
  canonical: CanonicalSessionProof; nonce: string; ownerProofSha256: string
}> {
  const probe = inputs.runtimeProbe
  if (!probe) throw new Error('XINGYUAN_INPUT_UNAVAILABLE')
  const nonce = randomUUID()
  const issuedAt = new Date().toISOString(), expiresAt = new Date(Date.now() + 60_000).toISOString()
  const directory = join(inputs.root, 'evidence/runtime-probe/edge/requests')
  privateDirectory(directory, inputs.root)
  const path = join(directory, `${nonce}.json`), temporary = join(directory, `.${nonce}.tmp`)
  const request = { schemaVersion: 1, runId: inputs.runId, manifestSha256: inputs.manifestSha256, role: 'edge',
    fixtureSha256: probe.fixtureSha256, artifactSha256: probe.artifactSha256, nonce, operation: 'EDGE_BINDING',
    tenantCode: realm, entityId: null, issuedAt, expiresAt }
  try {
    writeFileSync(temporary, JSON.stringify(request), { flag: 'wx', mode: 0o600 })
    const descriptor = openSync(temporary, 'r')
    try { fsyncSync(descriptor) } finally { closeSync(descriptor) }
    linkSync(temporary, path)
    unlinkSync(temporary)
    const parent = openSync(directory, 'r')
    try { fsyncSync(parent) } finally { closeSync(parent) }
  } finally { if (existsSync(temporary)) unlinkSync(temporary) }
  const responseStatus = await page.evaluate(async reference => {
    const response = await fetch('/bff/session', { credentials: 'include', cache: 'no-store',
      headers: { 'X-Xingyuan-Probe-Nonce': reference } })
    return response.status
  }, nonce)
  expect(responseStatus).toBe(200)
  const resultPath = join(inputs.root, 'evidence/runtime-probe/edge/results', `${nonce}.json`)
  await expect.poll(() => existsSync(resultPath), { timeout: Math.min(inputs.timeoutMs, 60_000), intervals: [100, 250, 500] }).toBe(true)
  const bytes = readPrivateFile(resultPath, inputs.root)
  const canonical = canonicalSessionProof(readIntegrationJson(bytes), { ...probe, runId: inputs.runId,
    manifestSha256: inputs.manifestSha256, nonce, issuer: client(inputs, realm, 'edge-ui').issuer,
    tenantId: inputs.tenantIds[realm], issuedAt, expiresAt })
  return { canonical, nonce, ownerProofSha256: createHash('sha256').update(bytes).digest('hex') }
}

async function mountedResource(page: Page, inputs: IntegrationInputs, realm: Realm, item: typeof children[number]): Promise<void> {
  // Enter through the real Portal link and Wujie HTML mount, retaining the same logical aliases in both realms.
  await page.goto('/')
  await page.locator(`a[href="${item.route}"]`).first().click()
  await expect(page.locator('wujie-app').first()).toBeAttached()
  expect(await status(page, item.bootstrap)).toBe(200)
  expect(await status(page, item.list)).toBe(200)
  await sessionMatches(page, inputs, realm)
}

async function noBrowserCredentials(page: Page): Promise<void> {
  const safe = await page.evaluate(() => {
    const jwt = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/
    return [localStorage, sessionStorage].every(storage => Object.keys(storage).every(key =>
      !/^(access[_-]?token|refresh[_-]?token|password)$/i.test(key) && !jwt.test(storage.getItem(key) ?? '')))
  })
  expect(safe).toBe(true)
  const cookies = await page.context().cookies()
  expect(cookies.some(cookie => cookie.name === '__Host-egon_bff' && cookie.httpOnly && cookie.secure && cookie.path === '/')).toBe(true)
}

async function csrfRejections(page: Page, inputs: IntegrationInputs, realm: Realm): Promise<void> {
  expect(await status(page, '/bff/logout', 'POST')).toBe(403)
  await sessionMatches(page, inputs, realm)
  const csrf = await page.evaluate(async () => {
    const response = await fetch('/bff/csrf', { credentials: 'include', cache: 'no-store' })
    if (!response.ok) throw new Error('CSRF_UNAVAILABLE')
    return await response.json() as { headerName: string; token: string }
  })
  const foreign = await page.request.post(`${inputs.origin}/bff/logout`, {
    headers: { Origin: 'https://foreign.invalid', [csrf.headerName]: csrf.token }, maxRedirects: 0,
  })
  expect(foreign.status()).toBe(403)
  await sessionMatches(page, inputs, realm)
}

async function actualUsageWindow(page: Page, inputs: IntegrationInputs, realm: Realm): Promise<{ fromInclusive: string; toExclusive: string }> {
  const fromInclusive = new Date().toISOString()
  // This original endpoint verifies each current held client and emits its existing USED observation.
  await sessionMatches(page, inputs, realm)
  return { fromInclusive, toExclusive: new Date(Date.now() + 1000).toISOString() }
}

async function logoutWithActualFamilyOracle(page: Page, inputs: IntegrationInputs, realm: Realm): Promise<void> {
  const name = realm === 'default' ? 'TENANT_A' : 'TENANT_B'
  marker(inputs, `${name}_HOLD`, 'READY', { realmCode: realm, aliases: ['edge-ui', ...children.map(value => value.alias)], ...await actualUsageWindow(page, inputs, realm) })
  await ownerReceipt(inputs, `${name}_HOLD`, 'READY')
  const code = await page.evaluate(async () => {
    const csrf = await (await fetch('/bff/csrf', { credentials: 'include', cache: 'no-store' })).json() as { headerName: string; token: string }
    const response = await fetch('/bff/logout', { method: 'POST', credentials: 'include', headers: { [csrf.headerName]: csrf.token } })
    return response.status
  })
  expect(code).toBe(204)
  marker(inputs, `${name}_LOGOUT`, 'CONFIRMED')
  // A cookie 401 alone never satisfies this gate: the SA owner must replay each same unexpired held AT.
  await ownerReceipt(inputs, `${name}_REPLAY`, 'PASSED')
  expect(await status(page, '/bff/session')).toBe(401)
  const cookies = await page.context().cookies()
  expect(cookies.some(cookie => cookie.name === '__Host-egon_bff')).toBe(false)
}

async function revocationFaultAndRecovery(page: Page, inputs: IntegrationInputs): Promise<void> {
  marker(inputs, 'TENANT_A_FAULT_BEGIN', 'READY', { realmCode: 'default', loginStartedAt: new Date().toISOString() })
  await authorize(page, inputs, 'default', 'edge-ui', '/', true)
  for (const item of children) {
    expect(await authorize(page, inputs, 'default', item.alias, item.route)).toBe(false)
    await mountedResource(page, inputs, 'default', item)
  }
  const before = taskControl(inputs, 'LOGOUT_QUEUE')
  expect(before.value.queuedCount).toBe(0)
  expect(before.value.pendingCount).toBe(0)
  const pending = await page.request.get(`${inputs.origin}/oauth2/authorization/edge-yuheng?returnTo=${encodeURIComponent(children[2].route)}`, { maxRedirects: 0 })
  expect(pending.status()).toBe(302)
  const lateLocation = pending.headers().location
  const late = new URL(lateLocation)
  const expected = client(inputs, 'default', 'edge-yuheng')
  expect(late.origin).toBe(inputs.origin)
  expect(late.pathname).toBe(`/tenants/${inputs.tenantIds.default}/oauth2/authorize`)
  expect(late.searchParams.get('client_id')).toBe(expected.clientId)
  expect(late.searchParams.get('code_challenge_method')).toBe('S256')
  expect(late.searchParams.get('state')).toBeTruthy()
  expect(late.searchParams.get('nonce')).toBeTruthy()
  marker(inputs, 'TENANT_A_FAULT_HOLD', 'READY', { realmCode: 'default', aliases: ['edge-ui', ...children.map(item => item.alias)],
    ...await actualUsageWindow(page, inputs, 'default') })
  await ownerReceipt(inputs, 'TENANT_A_FAULT_HOLD', 'READY')
  const controlProofs: { path: string; sha256: string }[] = [{ path: before.path, sha256: before.sha256 }]
  let restore = false
  let faultCompleted = false
  try {
    // Set the cleanup obligation before ON: an unknown reload outcome still requires an explicit owned OFF.
    restore = true
    const enabled = taskControl(inputs, 'REVOCATION_ON'); controlProofs.push({ path: enabled.path, sha256: enabled.sha256 })
    const failure = await page.evaluate(async () => {
      const csrf = await (await fetch('/bff/csrf', { credentials: 'include', cache: 'no-store' })).json() as { headerName: string; token: string }
      const response = await fetch('/bff/logout', { method: 'POST', credentials: 'include', headers: { [csrf.headerName]: csrf.token } })
      const body = await response.json() as { code?: string; localLoggedOut?: boolean; remoteState?: string }
      return { status: response.status, code: body.code, localLoggedOut: body.localLoggedOut, remoteState: body.remoteState }
    })
    expect(failure).toEqual({ status: 503, code: 'REMOTE_REVOCATION_PENDING', localLoggedOut: true, remoteState: 'PENDING' })
    expect(await status(page, '/bff/session')).toBe(401)
    expect((await page.context().cookies()).some(cookie => cookie.name === '__Host-egon_bff')).toBe(false)
    const queued = taskControl(inputs, 'LOGOUT_QUEUE'); controlProofs.push({ path: queued.path, sha256: queued.sha256 })
    expect(queued.value.queuedCount).toBe(5)
    let callbackObserved = false
    const observe = (request: import('@playwright/test').Request) => {
      const url = new URL(request.url())
      if (url.origin === inputs.origin && url.pathname === `/login/oauth2/code/${expected.registrationId}`
          && url.searchParams.has('code') && url.searchParams.get('state') === late.searchParams.get('state')) callbackObserved = true
    }
    page.on('request', observe)
    try {
      await page.goto(lateLocation)
      for (let attempt = 0; attempt < 5 && !callbackObserved; attempt++) {
        const url = new URL(page.url())
        if (url.origin !== inputs.origin || url.pathname.endsWith('/login')) throw new Error('XINGYUAN_VERIFICATION_FAILED')
        if (url.pathname === `/tenants/${inputs.tenantIds.default}/consent`) {
          const scopes = await page.locator('input[type="checkbox"][name="scope"]').evaluateAll(elements => elements.map(element => (element as HTMLInputElement).value))
          expect(scopes.every(scope => expected.allowedScopes.includes(scope))).toBe(true)
          for (const checkbox of await page.locator('input[type="checkbox"][name="scope"]').all()) await checkbox.check()
          await page.getByRole('button', { name: 'Allow selected permissions', exact: true }).click()
        } else await page.waitForTimeout(100)
      }
    } finally { page.off('request', observe) }
    expect(callbackObserved).toBe(true)
    expect(await status(page, '/bff/session')).toBe(401)
    expect((await page.context().cookies()).some(cookie => cookie.name === '__Host-egon_bff')).toBe(false)
    marker(inputs, 'AS_REVOCATION_PENDING', 'PASSED', { responseStatus: 503, remoteState: 'PENDING', localLoggedOut: true,
      durableIntentCount: 5, lateCallbackObserved: true, lateCallbackSessionStatus: 401, proofs: controlProofs })
    faultCompleted = true
  } finally {
    if (restore) {
      const disabled = taskControl(inputs, 'REVOCATION_OFF'); controlProofs.push({ path: disabled.path, sha256: disabled.sha256 })
    }
  }
  if (!faultCompleted) throw new Error('XINGYUAN_VERIFICATION_FAILED')
  let recovered: ReturnType<typeof taskControl> | undefined
  await expect.poll(() => {
    recovered = taskControl(inputs, 'LOGOUT_QUEUE')
    return [recovered.value.queuedCount, recovered.value.pendingCount]
  }, { timeout: 90_000, intervals: [500, 1000, 2500] }).toEqual([0, 0])
  if (!recovered) throw new Error('XINGYUAN_VERIFICATION_FAILED')
  controlProofs.push({ path: recovered.path, sha256: recovered.sha256 })
  marker(inputs, 'TENANT_A_FAULT_LOGOUT', 'CONFIRMED')
  await ownerReceipt(inputs, 'TENANT_A_FAULT_REPLAY', 'PASSED')
  expect(await status(page, '/bff/session')).toBe(401)
  expect((await page.context().cookies()).some(cookie => cookie.name === '__Host-egon_bff')).toBe(false)
  marker(inputs, 'AS_REVOCATION_RECOVERED', 'PASSED', { queuedCount: 0, pendingCount: 0, sessionStatus: 401, proofs: controlProofs })
}

test('LIVE-004/005/007/008/018/019/020: real A/B Code, actual-family replay and task revocation fault recovery', async ({ page }) => {
  let stage = 'INPUTS'
  try {
    const inputs = loadIntegrationInputs()
    if (!inputs.runtimeProbe) throw new Error('XINGYUAN_INPUT_UNAVAILABLE')
    await page.goto('/')
    expect(await status(page, '/bff/session')).toBe(401)
    let previousCanonical: CanonicalSessionProof | undefined
    for (const realm of ['default', 'tenant-b'] as const) {
      stage = `${realm === 'default' ? 'A' : 'B'}_CODE`
      marker(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_BEGIN`, 'READY', { realmCode: realm, loginStartedAt: new Date().toISOString() })
      await authorize(page, inputs, realm, 'edge-ui', '/', realm === 'tenant-b')
      const canonical = await captureCanonicalSession(page, inputs, realm)
      if (previousCanonical) {
        expect(canonical.canonical.bindingReference).not.toBe(previousCanonical.bindingReference)
        expect(Date.parse(canonical.canonical.originalCreatedAt)).toBeGreaterThanOrEqual(Date.parse(previousCanonical.originalCreatedAt))
      }
      const canonicalProofs = [{ phase: 'PORTAL', nonce: canonical.nonce, ownerProofSha256: canonical.ownerProofSha256 }]
      const logins: boolean[] = []
      for (const item of children) {
        logins.push(await authorize(page, inputs, realm, item.alias, item.route))
        await mountedResource(page, inputs, realm, item)
        const current = await captureCanonicalSession(page, inputs, realm)
        expect(current.canonical).toEqual(canonical.canonical)
        canonicalProofs.push({ phase: item.alias, nonce: current.nonce, ownerProofSha256: current.ownerProofSha256 })
      }
      expect(logins.every(value => !value)).toBe(true)
      await noBrowserCredentials(page)
      await csrfRejections(page, inputs, realm)
      if (realm === 'tenant-b') {
        stage = 'B_REJECT_A_REGISTRATION'
        const a = client(inputs, 'default', 'edge-ui')
        const rejected = await page.request.get(`${inputs.origin}/oauth2/authorization/${a.registrationId}`, { maxRedirects: 0 })
        expect(rejected.status()).toBe(400)
        await sessionMatches(page, inputs, realm)
      }
      stage = `${realm === 'default' ? 'A' : 'B'}_REFRESH`
      const refreshNotBefore = new Date().toISOString()
      await page.waitForTimeout(bffRefreshWaitMilliseconds(inputs.clients, realm))
      for (const item of children) await mountedResource(page, inputs, realm, item)
      await sessionMatches(page, inputs, realm)
      marker(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_REFRESH`, 'READY', { realmCode: realm, notBefore: refreshNotBefore, ...await actualUsageWindow(page, inputs, realm) })
      await ownerReceipt(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_REFRESH`, 'PASSED')
      const refreshed = await captureCanonicalSession(page, inputs, realm)
      expect(refreshed.canonical).toEqual(canonical.canonical)
      canonicalProofs.push({ phase: 'REFRESH', nonce: refreshed.nonce, ownerProofSha256: refreshed.ownerProofSha256 })
      stage = `${realm === 'default' ? 'A' : 'B'}_STEP_UP`
      const stepUpNotBefore = new Date().toISOString()
      await authorize(page, inputs, realm, 'edge-ui', '/', false, true)
      marker(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_STEP_UP`, 'READY', { realmCode: realm, notBefore: stepUpNotBefore, ...await actualUsageWindow(page, inputs, realm) })
      await ownerReceipt(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_STEP_UP`, 'PASSED')
      const steppedUp = await captureCanonicalSession(page, inputs, realm)
      expect(steppedUp.canonical).toEqual(canonical.canonical)
      canonicalProofs.push({ phase: 'STEP_UP', nonce: steppedUp.nonce, ownerProofSha256: steppedUp.ownerProofSha256 })
      marker(inputs, `${realm === 'default' ? 'TENANT_A' : 'TENANT_B'}_CANONICAL`, 'PASSED', { canonical: canonical.canonical, proofs: canonicalProofs })
      stage = `${realm === 'default' ? 'A' : 'B'}_LOGOUT`
      await logoutWithActualFamilyOracle(page, inputs, realm)
      previousCanonical = canonical.canonical
    }
    stage = 'AS_REVOCATION_FAULT'
    await revocationFaultAndRecovery(page, inputs)
    marker(inputs, 'AB_BROWSER', 'PASSED')
  } catch {
    // Playwright call logs can contain form input or OAuth callback values; expose only the fixed gate stage.
    throw new Error(`XINGYUAN_VERIFICATION_FAILED:${stage}`)
  }
})


async function boundedJson(url: string, init: RequestInit = {}): Promise<Record<string, unknown>> {
  const response = await fetch(url, { ...init, redirect: 'error', signal: AbortSignal.timeout(30_000) })
  if (!response.ok) throw new Error(response.status >= 500 ? 'XINGYUAN_DEPENDENCY_UNAVAILABLE' : 'XINGYUAN_VERIFICATION_FAILED')
  if (!response.body) throw new Error('XINGYUAN_DEPENDENCY_UNAVAILABLE')
  const reader = response.body.getReader()
  const chunks: Uint8Array[] = []
  let size = 0
  try {
    while (true) {
      const next = await reader.read()
      if (next.done) break
      size += next.value.length
      if (size > 128 * 1024) throw new Error('XINGYUAN_INPUT_INVALID')
      chunks.push(next.value)
    }
  } finally {
    await reader.cancel()
  }
  return readIntegrationJson(Buffer.concat(chunks))
}

function verifiedClaims(token: string, keys: unknown[], issuer: string, audience: string,
                        type: 'JWT' | 'at+jwt'): Record<string, unknown> {
  if (token.length > 128 * 1024 || !/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(token)) {
    throw new Error('XINGYUAN_VERIFICATION_FAILED')
  }
  const segments = token.split('.')
  const header = readIntegrationJson(Buffer.from(segments[0], 'base64url'))
  if (header.alg !== 'RS256' || header.typ !== type || typeof header.kid !== 'string' || header.crit !== undefined) {
    throw new Error('XINGYUAN_VERIFICATION_FAILED')
  }
  const candidates = keys.filter(value => value && typeof value === 'object'
    && (value as Record<string, unknown>).kid === header.kid && (value as Record<string, unknown>).kty === 'RSA'
    && (value as Record<string, unknown>).alg === 'RS256' && (value as Record<string, unknown>).use === 'sig')
  if (candidates.length !== 1) throw new Error('XINGYUAN_VERIFICATION_FAILED')
  if (['d', 'p', 'q', 'dp', 'dq', 'qi'].some(field => field in (candidates[0] as Record<string, unknown>))) throw new Error('XINGYUAN_VERIFICATION_FAILED')
  const key = createPublicKey({ key: candidates[0] as import('node:crypto').JsonWebKeyInput['key'], format: 'jwk' })
  if ((key.asymmetricKeyDetails?.modulusLength ?? 0) < 3072
      || !verifySignature('RSA-SHA256', Buffer.from(`${segments[0]}.${segments[1]}`), key, Buffer.from(segments[2], 'base64url'))) {
    throw new Error('XINGYUAN_VERIFICATION_FAILED')
  }
  const claims = readIntegrationJson(Buffer.from(segments[1], 'base64url'))
  const audiences = Array.isArray(claims.aud) ? claims.aud : [claims.aud]
  const now = Math.floor(Date.now() / 1000)
  if (claims.iss !== issuer || audiences.length !== 1 || audiences[0] !== audience
      || !Number.isInteger(claims.exp) || (claims.exp as number) <= now
      || !Number.isInteger(claims.iat) || (claims.iat as number) > now + 30
      || (claims.nbf !== undefined && (!Number.isInteger(claims.nbf) || (claims.nbf as number) > now + 30))) {
    throw new Error('XINGYUAN_VERIFICATION_FAILED')
  }
  return claims
}

function publishPrivate(inputs: IntegrationInputs, path: string, bytes: Buffer, replaceIndex = false): void {
  if (!path.startsWith(`${inputs.root}/secrets/`) && !path.startsWith(`${inputs.root}/evidence/`)) throw new Error('XINGYUAN_TARGET_UNSAFE')
  const parent = path.slice(0, path.lastIndexOf('/'))
  privateDirectory(parent, inputs.root)
  const temporary = join(parent, `.cli-${randomUUID()}.tmp`)
  if (replaceIndex && path !== join(inputs.root, 'secrets/token-index.json')) throw new Error('XINGYUAN_TARGET_UNSAFE')
  try {
    writeFileSync(temporary, bytes, { mode: 0o600, flag: 'wx' })
    const frame = openSync(temporary, 'r')
    try { fsyncSync(frame) } finally { closeSync(frame) }
    if (replaceIndex) renameSync(temporary, path)
    else { linkSync(temporary, path); unlinkSync(temporary) }
    const directory = openSync(parent, 'r')
    try { fsyncSync(directory) } finally { closeSync(directory) }
  } finally {
    if (existsSync(temporary)) unlinkSync(temporary)
  }
}

async function publicConsent(page: Page, inputs: IntegrationInputs, selected: IntegrationPublicClientFact,
                             user: 'admin' | 'viewer', authorizationUri: string, callbackOrigin: string): Promise<void> {
  await page.goto(authorizationUri)
  for (let stage = 0; stage < 5; stage++) {
    const current = new URL(page.url())
    if (current.origin === callbackOrigin) return
    if (current.origin !== inputs.origin) throw new Error('XINGYUAN_TARGET_UNSAFE')
    if (current.pathname.endsWith('/login')) {
      await page.getByLabel('Username', { exact: true }).fill(user === 'admin' ? 'alice' : 'viewer')
      const password = readPrivateFile(inputs.secretFiles[`${user}-password`], join(inputs.root, 'secrets'), 4096)
      try {
        await page.getByLabel('Password', { exact: true }).fill(password.toString('utf8').replace(/\r?\n$/, ''))
        await page.getByRole('button', { name: 'Sign in', exact: true }).click()
      } finally { password.fill(0) }
    } else if (current.pathname.endsWith('/consent')) {
      const scopes = await page.locator('input[type="checkbox"][name="scope"]').evaluateAll(elements =>
        elements.map(element => (element as HTMLInputElement).value))
      if (!scopes.every(scope => selected.scopes.includes(scope))) throw new Error('XINGYUAN_TARGET_UNSAFE')
      for (const checkbox of await page.locator('input[type="checkbox"][name="scope"]').all()) await checkbox.check()
      await page.getByRole('button', { name: 'Allow selected permissions', exact: true }).click()
    } else {
      await page.waitForTimeout(100)
    }
  }
  if (new URL(page.url()).origin !== callbackOrigin) throw new Error('XINGYUAN_VERIFICATION_FAILED')
}

test('CLI-006: isolated PUBLIC Code/S256 input with exact client/resource and a loopback callback', async ({ page }) => {
  let stage = 'INPUTS'
  let server: ReturnType<typeof createServer> | undefined
  let timer: ReturnType<typeof setTimeout> | undefined
  try {
    const inputs = loadIntegrationInputs()
    const realm = process.env.XINGYUAN_CLI_TENANT_CODE
    const resource = process.env.XINGYUAN_CLI_RESOURCE_KEY
    const user = process.env.XINGYUAN_CLI_USER_KEY
    const outputFile = process.env.XINGYUAN_CLI_OUTPUT_FILE
    const resultFile = process.env.XINGYUAN_CLI_RESULT_FILE
    const scopeMode = process.env.XINGYUAN_CLI_SCOPE_MODE
    if (scopeMode !== undefined && scopeMode !== 'IDENTITY_ONLY' || scopeMode === 'IDENTITY_ONLY' && user !== 'admin') throw new Error('XINGYUAN_TARGET_UNSAFE')
    const scopeOnly = scopeMode === 'IDENTITY_ONLY'
    if ((realm !== 'default' && realm !== 'tenant-b') || (user !== 'admin' && user !== 'viewer') || !resource || !outputFile || !resultFile) {
      throw new Error('XINGYUAN_INPUT_UNAVAILABLE')
    }
    const selected = inputs.publicClients.filter(value => value.realmCode === realm && value.resourceKey === resource)
    if (selected.length !== 1) throw new Error('XINGYUAN_TARGET_UNSAFE')
    if (scopeOnly && !selected[0].scopes.includes('openid')) throw new Error('XINGYUAN_INPUT_UNAVAILABLE')
    const target = scopeOnly ? { ...selected[0], scopes: ['openid'] } : selected[0]
    if (!outputFile.startsWith(`${inputs.root}/secrets/`) || existsSync(outputFile)
        || outputFile === join(inputs.root, 'secrets/token-index.json') || existsSync(resultFile)) throw new Error('XINGYUAN_STATE_CONFLICT')
    const canonicalIndex = join(inputs.root, 'secrets/token-index.json')
    const canonicalBefore = scopeOnly ? readPrivateFile(canonicalIndex, join(inputs.root, 'secrets')) : undefined
    const indexFile = scopeOnly ? `${outputFile}.metadata.json` : canonicalIndex
    if (scopeOnly && existsSync(indexFile)) throw new Error('XINGYUAN_STATE_CONFLICT')
    const previous = existsSync(indexFile) ? readPrivateFile(indexFile, join(inputs.root, 'secrets')) : undefined
    const index = previous ? readIntegrationJson(previous) : { schemaVersion: 1, runId: inputs.runId, manifestSha256: inputs.manifestSha256, tokens: [] }
    if (index.schemaVersion !== 1 || index.runId !== inputs.runId || index.manifestSha256 !== inputs.manifestSha256
        || !Array.isArray(index.tokens) || index.tokens.length > 192) throw new Error('XINGYUAN_TARGET_UNSAFE')
    stage = 'DISCOVERY'
    const discovery = await boundedJson(`${target.issuer}/.well-known/openid-configuration`)
    if (discovery.issuer !== target.issuer || discovery.authorization_endpoint !== `${target.issuer}/oauth2/authorize`
        || discovery.token_endpoint !== `${target.issuer}/oauth2/token` || discovery.jwks_uri !== `${target.issuer}/.well-known/jwks.json`
        || discovery.userinfo_endpoint !== `${target.issuer}/userinfo`) throw new Error('XINGYUAN_TARGET_UNSAFE')
    const jwks = await boundedJson(discovery.jwks_uri)
    if (!Array.isArray(jwks.keys) || jwks.keys.length > 32) throw new Error('XINGYUAN_TARGET_UNSAFE')
    const callback = new URL(target.redirectUri)
    const verifier = randomBytes(64).toString('base64url')
    const challenge = createHash('sha256').update(verifier).digest('base64url')
    const state = randomBytes(32).toString('base64url')
    const nonce = randomBytes(32).toString('base64url')
    let accepted = false
    let resolveCode!: (code: string) => void
    let rejectCode!: (error: Error) => void
    const received = new Promise<string>((resolve, reject) => { resolveCode = resolve; rejectCode = reject })
    // Attach a handler immediately so a rejected callback never becomes an unhandled token-bearing error.
    void received.catch(() => undefined)
    const privateKey = readPrivateFile(inputs.secretFiles['tls-server-key'], join(inputs.root, 'secrets'))
    try {
      server = createServer({ key: privateKey, cert: readPrivateFile(inputs.secretFiles['tls-server-cert'], join(inputs.root, 'secrets')) }, (request, response) => {
        response.setHeader('Cache-Control', 'no-store')
        response.setHeader('Referrer-Policy', 'no-referrer')
        let incoming: URL
        try { incoming = new URL(request.url ?? '/', callback.origin) } catch {
          response.writeHead(400).end('Callback rejected')
          rejectCode(new Error('XINGYUAN_TARGET_UNSAFE'))
          return
        }
        if (incoming.pathname === '/complete' && !incoming.search) { response.end('Authorization recorded'); return }
        if (incoming.pathname !== callback.pathname) { response.writeHead(404).end(); return }
        if (accepted) { response.writeHead(409).end('Callback already consumed'); return }
        if (incoming.searchParams.has('error')) {
          response.writeHead(400).end('Authorization was not completed')
          rejectCode(new Error('XINGYUAN_VERIFICATION_FAILED'))
          return
        }
        if (request.method !== 'GET' || incoming.origin !== callback.origin || request.headers.host !== callback.host
            || [...incoming.searchParams.keys()].some(key => !['code', 'state', 'iss'].includes(key))
            || ['code', 'state', 'iss'].some(key => incoming.searchParams.getAll(key).length !== 1)
            || incoming.searchParams.get('state') !== state || incoming.searchParams.get('iss') !== target.issuer
            || !incoming.searchParams.get('code')) {
          response.writeHead(400).end('Callback rejected')
          rejectCode(new Error('XINGYUAN_TARGET_UNSAFE'))
          return
        }
        accepted = true
        resolveCode(incoming.searchParams.get('code')!)
        response.writeHead(303, { Location: '/complete' }).end()
      })
    } finally { privateKey.fill(0) }
    stage = 'CALLBACK'
    await new Promise<void>((resolve, reject) => {
      server!.once('error', () => reject(new Error('XINGYUAN_INPUT_UNAVAILABLE')))
      server!.listen(Number(callback.port), '127.0.0.1', resolve)
    })
    timer = setTimeout(() => rejectCode(new Error('XINGYUAN_DEPENDENCY_UNAVAILABLE')), inputs.timeoutMs)
    const request = new URL(`${target.issuer}/oauth2/authorize`)
    request.search = new URLSearchParams({ response_type: 'code', client_id: target.clientId, redirect_uri: target.redirectUri,
      scope: target.scopes.join(' '), resource: target.resourceUri, state, nonce, code_challenge: challenge, code_challenge_method: 'S256' }).toString()
    await publicConsent(page, inputs, target, user, request.toString(), callback.origin)
    const code = await received
    clearTimeout(timer)
    stage = 'TOKEN'
    const token = await boundedJson(`${target.issuer}/oauth2/token`, { method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ grant_type: 'authorization_code',
        client_id: target.clientId, redirect_uri: target.redirectUri, code, code_verifier: verifier, resource: target.resourceUri }) })
    if (token.token_type !== 'Bearer' || typeof token.access_token !== 'string' || typeof token.id_token !== 'string'
        || token.refresh_token !== undefined) throw new Error('XINGYUAN_VERIFICATION_FAILED')
    stage = 'VERIFY'
    const identity = verifiedClaims(token.id_token, jwks.keys, target.issuer, target.clientId, 'JWT')
    const access = verifiedClaims(token.access_token, jwks.keys, target.issuer, target.resourceUri, 'at+jwt')
    if (identity.nonce !== nonce || identity.sub !== inputs.subjects[user] || access.sub !== inputs.subjects[user]
        || access.client_id !== target.clientId || access.client_registration_id !== target.clientPk || access.tenant_id !== inputs.tenantIds[realm] || access.principal_type !== 'USER'
        || access.scope_context !== 'TENANT' || typeof access.scope !== 'string'
        || (access.scope as string).split(' ').length !== target.scopes.length
        || target.scopes.some(scope => !(access.scope as string).split(' ').includes(scope))) throw new Error('XINGYUAN_VERIFICATION_FAILED')
    const headers = { Authorization: `Bearer ${token.access_token}` }
    const profile = await boundedJson(`${target.issuer}/userinfo`, { headers })
    if (profile.sub !== inputs.subjects[user]) throw new Error('XINGYUAN_VERIFICATION_FAILED')
    // Resource authorization is verified after consumers start; this phase only publishes AS-verified inputs.
    stage = 'PUBLISH'
    const bytes = Buffer.from(token.access_token, 'utf8')
    try { publishPrivate(inputs, outputFile, bytes) } finally { bytes.fill(0) }
    const tokens = index.tokens.filter(value => {
      if (!value || typeof value !== 'object') throw new Error('XINGYUAN_TARGET_UNSAFE')
      const item = value as Record<string, unknown>
      return !(item.tenantCode === realm && item.resourceKey === resource && item.userKey === user)
    })
    tokens.push({ tenantCode: realm, tenantId: inputs.tenantIds[realm], resourceKey: resource, userKey: user,
      clientId: target.clientId, clientPk: target.clientPk, issuer: target.issuer, resourceUri: target.resourceUri,
      scopes: (access.scope as string).split(' ').sort(), tokenFile: outputFile, probeStatus: null,
      tokenSha256: createHash('sha256').update(token.access_token).digest('hex'), expiresAt: new Date((access.exp as number) * 1000).toISOString() })
    if (previous ? !existsSync(indexFile) || !readPrivateFile(indexFile, join(inputs.root, 'secrets')).equals(previous) : existsSync(indexFile)) {
      throw new Error('XINGYUAN_STATE_CONFLICT')
    }
    if (scopeOnly) {
      if (!canonicalBefore || !readPrivateFile(canonicalIndex, join(inputs.root, 'secrets')).equals(canonicalBefore) || tokens.length !== 1) throw new Error('XINGYUAN_STATE_CONFLICT')
      publishPrivate(inputs, indexFile, Buffer.from(JSON.stringify({ schemaVersion: 1, runId: inputs.runId, manifestSha256: inputs.manifestSha256,
        scope: 'IDENTITY_ONLY_PUBLIC_CODE', token: tokens[0] })))
    } else publishPrivate(inputs, indexFile, Buffer.from(JSON.stringify({ ...index, tokens })), Boolean(previous))
    publishPrivate(inputs, resultFile, Buffer.from(JSON.stringify({ schemaVersion: 1, operationId: 'CLI-006', runId: inputs.runId,
      manifestSha256: inputs.manifestSha256, status: 'COMMITTED', completedAt: new Date().toISOString(),
      targets: [{ targetKey: resource, identity: 'PUBLIC_USER', version: '1', state: 'COMMITTED' }],
      counts: { databaseCount: 0, tenantCount: 1, clientCount: 1, catalogCount: 0, liveCaseCount: 0 }, steps: [], error: null })))
  } catch (error) {
    const codes = ['XINGYUAN_TARGET_UNSAFE', 'XINGYUAN_INPUT_UNAVAILABLE', 'XINGYUAN_INPUT_INVALID',
      'XINGYUAN_DEPENDENCY_UNAVAILABLE', 'XINGYUAN_STATE_CONFLICT']
    const code = error instanceof Error && codes.includes(error.message) ? error.message : 'XINGYUAN_VERIFICATION_FAILED'
    throw new Error(`${code}:${stage}`)
  } finally {
    if (timer) clearTimeout(timer)
    if (server?.listening) await new Promise<void>((resolve, reject) => server!.close(error => error ? reject(new Error('XINGYUAN_PARTIAL')) : resolve()))
  }
})
