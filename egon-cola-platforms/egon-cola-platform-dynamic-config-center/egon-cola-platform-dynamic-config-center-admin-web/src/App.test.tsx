import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the login page when no token is stored', () => {
    sessionStorage.clear()
    render(<App />)
    expect(screen.getByText('连接本机 DDC 管理端')).toBeInTheDocument()
  })
})
