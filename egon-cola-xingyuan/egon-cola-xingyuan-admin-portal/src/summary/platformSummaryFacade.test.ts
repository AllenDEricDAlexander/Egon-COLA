import { describe, expect, it, vi } from 'vitest'
import {
  loadSummary,
  retrySummary,
  type SummaryAdapters,
} from './platformSummaryFacade'

describe('xingyuan summary facade', () => {
  it('keeps other cards visible when one adapter times out', async () => {
    const result = await loadSummary(
      { scope: 'default' },
      new AbortController().signal,
      {
        tianquan-shoubing: vi.fn().mockResolvedValue({ status: 'READY', summary: '12 users' }),
        tianquan-jianshen: vi.fn().mockImplementation(() => new Promise(() => undefined)),
        tianshu: vi.fn().mockResolvedValue({ status: 'READY', summary: '8 configs' }),
      },
      { timeoutMs: 5 },
    )

    expect(result.overall).toBe('PARTIAL')
    expect(result.cards.tianquan-shoubing.status).toBe('READY')
    expect(result.cards.tianquan-jianshen.status).toBe('TIMEOUT')
    expect(result.cards.tianshu.status).toBe('READY')
    expect(result.cards.yuheng.status).toBe('NOT_CONFIGURED')
  })

  it('retries only the selected xingyuan adapter', async () => {
    const tianquan-shoubing = vi.fn().mockResolvedValue({ status: 'READY', summary: '12 users' })
    const yuheng = vi.fn()
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValueOnce({ status: 'READY', summary: 'healthy' })
    const adapters: SummaryAdapters = { tianquan-shoubing, yuheng }

    await loadSummary({ scope: 'default' }, new AbortController().signal, adapters)
    const card = await retrySummary(
      'yuheng',
      { scope: 'default' },
      new AbortController().signal,
      adapters,
    )

    expect(card.status).toBe('READY')
    expect(yuheng).toHaveBeenCalledTimes(2)
    expect(tianquan-shoubing).toHaveBeenCalledTimes(1)
  })
})
