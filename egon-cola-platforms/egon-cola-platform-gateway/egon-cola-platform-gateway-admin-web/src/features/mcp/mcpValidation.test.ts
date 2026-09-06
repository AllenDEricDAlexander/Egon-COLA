import {describe, expect, it} from 'vitest'
import {renderPromptTemplate} from './mcpValidation'

describe('strict Prompt preview contract', () => {
  it('uses the runtime dollar-brace syntax and leaves other text literal', () => {
    expect(renderPromptTemplate('检查 ${orderId} / ${orderId}; {{orderId}}', ['orderId']))
      .toBe('检查 [orderId] / [orderId]; {{orderId}}')
  })

  it.each(['${unknown}', '${ orderId }', '${orderId.toUpperCase()}', '${orderId'])
  ('rejects an undeclared or malformed runtime expression: %s', (template) => {
    expect(() => renderPromptTemplate(template, ['orderId'])).toThrow()
  })

  it('applies runtime argument declaration and template limits', () => {
    expect(() => renderPromptTemplate('literal', ['x', 'x'])).toThrow()
    expect(() => renderPromptTemplate('literal', ['_invalid'])).toThrow()
    expect(() => renderPromptTemplate('x'.repeat(256 * 1024 + 1), [])).toThrow()
    expect(renderPromptTemplate('literal', [])).toBe('literal')
  })
})
