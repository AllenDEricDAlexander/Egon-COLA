import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import {describe, expect, it, vi} from 'vitest'
import {ManagementPolicyEditor} from './ManagementPolicyEditor'
import type {ManagementPolicyView} from './managementPolicy.api'

const policy: ManagementPolicyView = {
  policyId: '101', policyCode: 'QA_POLICY', name: '限时委托策略', status: 'ACTIVE',
  validFrom: new Date(2026, 8, 6, 13, 0, 12, 345).toISOString(),
  validTo: new Date(2026, 8, 6, 14, 0, 23, 456).toISOString(),
  subjects: [{type: 'USER', id: '7'}], scopes: [{type: 'CUSTOM_USER', referenceId: '8'}],
  activationRootRoleIds: ['3'], operations: ['ASSIGN_ROLE'], version: 1,
  restrictions: {
    maximumAssignmentDays: 1, maximumRiskLevel: 'MEDIUM', requiredAuthenticationStrength: 'PASSWORD',
    requireReason: true, requireTicket: false, includeInheritedSubjectRoles: false,
    requireAllAffiliationsInScope: true,
  },
}

describe('management policy editor', () => {
  it('shows local date-times and preserves the original instants including milliseconds on save', async () => {
    const onSave = vi.fn()
    render(<ManagementPolicyEditor open policy={policy} saving={false} onCancel={vi.fn()} onSave={onSave} />)

    expect(screen.getByLabelText('生效时间')).toHaveValue('2026-09-06T13:00:12.345')
    expect(screen.getByLabelText('失效时间')).toHaveValue('2026-09-06T14:00:23.456')
    fireEvent.click(screen.getByRole('button', {name: '完整保存策略'}))

    await waitFor(() => expect(onSave).toHaveBeenCalledWith(expect.objectContaining({
      validFrom: policy.validFrom, validTo: policy.validTo,
    })))
  })

  it('offers only authentication strengths accepted by the backend', async () => {
    const onSave = vi.fn()
    render(<ManagementPolicyEditor open policy={{...policy, validTo: null}} saving={false} onCancel={vi.fn()} onSave={onSave} />)

    fireEvent.mouseDown(screen.getByRole('combobox', {name: '最低认证强度'}))
    fireEvent.click(await screen.findByText('STRONG', {selector: '.ant-select-item-option-content'}))
    expect(screen.queryByText('HARDWARE_KEY')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', {name: '完整保存策略'}))

    await waitFor(() => expect(onSave).toHaveBeenCalledWith(expect.objectContaining({
      validTo: null, restrictions: expect.objectContaining({requiredAuthenticationStrength: 'STRONG'}),
    })))
  })
})
