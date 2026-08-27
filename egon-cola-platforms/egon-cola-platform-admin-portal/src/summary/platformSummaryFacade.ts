import type { PlatformKey } from '../manifest/types'

export const PLATFORM_KEYS: readonly PlatformKey[] = ['idp', 'rbac3', 'gateway', 'ddc']

const platformLabels: Record<PlatformKey, string> = {
  idp: '身份与安全',
  rbac3: '权限治理',
  gateway: 'API 网关',
  ddc: '配置中心',
}

export type SummaryCardStatus =
  | 'READY'
  | 'DEGRADED'
  | 'ERROR'
  | 'TIMEOUT'
  | 'NOT_CONFIGURED'
  | 'DENIED'

export interface SummaryContext {
  readonly scope?: string
}

export interface SummaryAdapterResult {
  readonly status?: SummaryCardStatus
  readonly summary?: string
  readonly message?: string
  readonly traceId?: string
}

export type PlatformSummaryAdapter = (
  context: SummaryContext,
  signal: AbortSignal,
) => Promise<SummaryAdapterResult>

export type SummaryAdapters = Partial<Record<PlatformKey, PlatformSummaryAdapter>>

export interface PlatformSummaryCard {
  readonly platformKey: PlatformKey
  readonly label: string
  readonly status: SummaryCardStatus
  readonly durationMs: number
  readonly summary?: string
  readonly message?: string
  readonly traceId?: string
}

export interface PlatformSummary {
  readonly overall: 'READY' | 'PARTIAL'
  readonly cards: Record<PlatformKey, PlatformSummaryCard>
}

export type SummaryFailureCode = 'TIMEOUT' | 'INVALID_RESPONSE'

export class SummaryAdapterError extends Error {
  readonly code: SummaryFailureCode
  readonly status?: number

  constructor(code: SummaryFailureCode, message: string, status?: number) {
    super(message)
    this.name = 'SummaryAdapterError'
    this.code = code
    this.status = status
  }
}

export interface SummaryLoadOptions {
  readonly timeoutMs?: number
}

const DEFAULT_TIMEOUT_MS = 3_000

const abortError = (): DOMException => new DOMException('The operation was aborted', 'AbortError')

const elapsed = (startedAt: number): number => Math.max(0, Math.round(performance.now() - startedAt))

const failureCard = (
  platformKey: PlatformKey,
  status: SummaryCardStatus,
  durationMs: number,
  message: string,
): PlatformSummaryCard => ({
  platformKey,
  label: platformLabels[platformKey],
  status,
  durationMs,
  message,
})

const statusFromFailure = (error: unknown): { status: SummaryCardStatus; message: string } => {
  if (error instanceof SummaryAdapterError && error.code === 'TIMEOUT') {
    return { status: 'TIMEOUT', message: '摘要请求超时，可单独重试' }
  }
  const status = error instanceof SummaryAdapterError
    ? error.status
    : (error && typeof error === 'object' && 'status' in error && typeof error.status === 'number'
      ? error.status
      : undefined)
  if (status === 401 || status === 403) {
    return { status: 'DENIED', message: '当前账号无权访问该平台摘要' }
  }
  if (status === 404) {
    return { status: 'NOT_CONFIGURED', message: '该平台摘要接口尚未配置' }
  }
  return { status: 'ERROR', message: '平台摘要加载失败，可单独重试' }
}

const invokeAdapter = async (
  adapter: PlatformSummaryAdapter,
  context: SummaryContext,
  signal: AbortSignal,
  timeoutMs: number,
): Promise<SummaryAdapterResult> => {
  if (signal.aborted) {
    throw signal.reason ?? abortError()
  }

  const controller = new AbortController()
  const abortChild = (): void => controller.abort(signal.reason ?? abortError())
  signal.addEventListener('abort', abortChild, { once: true })
  let timeoutHandle: ReturnType<typeof setTimeout> | undefined
  try {
    const timeout = new Promise<never>((_resolve, reject) => {
      timeoutHandle = setTimeout(() => {
        controller.abort()
        reject(new SummaryAdapterError('TIMEOUT', 'Summary adapter timed out'))
      }, timeoutMs)
    })
    const response = await Promise.race([adapter(context, controller.signal), timeout])
    if (!response || typeof response !== 'object') {
      throw new SummaryAdapterError('INVALID_RESPONSE', 'Summary adapter returned an invalid response')
    }
    return response
  } finally {
    if (timeoutHandle !== undefined) {
      clearTimeout(timeoutHandle)
    }
    signal.removeEventListener('abort', abortChild)
  }
}

const loadCard = async (
  platformKey: PlatformKey,
  context: SummaryContext,
  signal: AbortSignal,
  adapter: PlatformSummaryAdapter | undefined,
  options: SummaryLoadOptions,
): Promise<PlatformSummaryCard> => {
  const startedAt = performance.now()
  if (!adapter) {
    return failureCard(platformKey, 'NOT_CONFIGURED', elapsed(startedAt), '该平台摘要尚未配置')
  }

  try {
    const result = await invokeAdapter(adapter, context, signal, options.timeoutMs ?? DEFAULT_TIMEOUT_MS)
    const status = result.status ?? 'READY'
    return {
      platformKey,
      label: platformLabels[platformKey],
      status,
      durationMs: elapsed(startedAt),
      ...(typeof result.summary === 'string' ? { summary: result.summary } : {}),
      ...(typeof result.message === 'string' ? { message: result.message } : {}),
      ...(typeof result.traceId === 'string' ? { traceId: result.traceId } : {}),
    }
  } catch (error) {
    if (signal.aborted) {
      throw error
    }
    const failure = statusFromFailure(error)
    return failureCard(platformKey, failure.status, elapsed(startedAt), failure.message)
  }
}

const buildSummary = (cards: Record<PlatformKey, PlatformSummaryCard>): PlatformSummary => ({
  cards,
  overall: PLATFORM_KEYS.every((platformKey) => cards[platformKey].status === 'READY') ? 'READY' : 'PARTIAL',
})

export const loadSummary = async (
  context: SummaryContext,
  signal: AbortSignal,
  adapters: SummaryAdapters,
  options: SummaryLoadOptions = {},
): Promise<PlatformSummary> => {
  const settled = await Promise.allSettled(
    PLATFORM_KEYS.map((platformKey) => loadCard(platformKey, context, signal, adapters[platformKey], options)),
  )
  const cards = {} as Record<PlatformKey, PlatformSummaryCard>
  settled.forEach((result, index) => {
    const platformKey = PLATFORM_KEYS[index]
    if (result.status === 'rejected') {
      throw result.reason
    }
    cards[platformKey] = result.value
  })
  return buildSummary(cards)
}

export const retrySummary = async (
  platformKey: PlatformKey,
  context: SummaryContext,
  signal: AbortSignal,
  adapters: SummaryAdapters,
  options: SummaryLoadOptions = {},
): Promise<PlatformSummaryCard> => loadCard(platformKey, context, signal, adapters[platformKey], options)
