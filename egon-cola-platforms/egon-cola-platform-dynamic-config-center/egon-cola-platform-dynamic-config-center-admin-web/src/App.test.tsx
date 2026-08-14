import {render, screen} from '@testing-library/react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('renders the unified login page when silent refresh is unavailable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('no session')))
    render(<App />)
    expect(await screen.findByText('DDC 管理端')).toBeInTheDocument()
  })
})
