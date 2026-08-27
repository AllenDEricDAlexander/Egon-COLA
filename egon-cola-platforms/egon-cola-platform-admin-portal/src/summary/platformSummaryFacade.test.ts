import { describe, expect, it, vi } from 'vitest'
import {
  loadSummary,
  retrySummary,
  type SummaryAdapters,
} from './platformSummaryFacade'

describe('platform summary facade', () => {
  it('keeps other cards visible when one adapter times out', async () => {
    const result = await loadSummary(
      { scope: 'default' },
      new AbortController().signal,
      {
        idp: vi.fn().mockResolvedValue({ status: 'READY', summary: '12 users' }),
        rbac3: vi.fn().mockImplementation(() => new Promise(() => undefined)),
        ddc: vi.fn().mockResolvedValue({ status: 'READY', summary: '8 configs' }),
      },
      { timeoutMs: 5 },
    )

    expect(result.overall).toBe('PARTIAL')
    expect(result.cards.idp.status).toBe('READY')
    expect(result.cards.rbac3.status).toBe('TIMEOUT')
    expect(result.cards.ddc.status).toBe('READY')
    expect(result.cards.gateway.status).toBe('NOT_CONFIGURED')
  })

  it('retries only the selected platform adapter', async () => {
    const idp = vi.fn().mockResolvedValue({ status: 'READY', summary: '12 users' })
    const gateway = vi.fn()
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValueOnce({ status: 'READY', summary: 'healthy' })
    const adapters: SummaryAdapters = { idp, gateway }

    await loadSummary({ scope: 'default' }, new AbortController().signal, adapters)
    const card = await retrySummary(
      'gateway',
      { scope: 'default' },
      new AbortController().signal,
      adapters,
    )

    expect(card.status).toBe('READY')
    expect(gateway).toHaveBeenCalledTimes(2)
    expect(idp).toHaveBeenCalledTimes(1)
  })
})
