import { Button, Card, Form, Input, Modal, Space, Table, Tag, message } from 'antd'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth, httpClient } from '../../auth/AuthContext'
import { usePermission, PageState } from '@egon-cola/admin-web-shared'
import type { IdentityUser } from '../../api/types'

export const UserListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<{ username: string; displayName: string }>()
  const [messageApi, contextHolder] = message.useMessage()

  const usersQuery = useQuery({
    queryKey: ['idp', 'users'],
    queryFn: () => httpClient.request<IdentityUser[]>('/api/v1/identity/users'),
  })

  const createMutation = useMutation({
    mutationFn: (values: { username: string; displayName: string }) =>
      httpClient.request<{ oneTimePassword: string }>('/api/v1/identity/users', {
        method: 'POST',
        body: JSON.stringify(values),
      }),
    onSuccess: async (result) => {
      setModalOpen(false)
      form.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
      Modal.success({ title: '用户已创建', content: `一次性密码：${result.oneTimePassword}（关闭后不再显示）` })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '创建失败') },
  })

  const resetPasswordMutation = useMutation({
    mutationFn: (subject: string) =>
      httpClient.request<{ oneTimePassword: string }>(
        `/api/v1/identity/users/${encodeURIComponent(subject)}/password-reset`,
        { method: 'POST' },
      ),
    onSuccess: (result) => { Modal.success({ title: '密码已重置', content: `一次性密码：${result.oneTimePassword}` }) },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '重置失败') },
  })

  const revokeMutation = useMutation({
    mutationFn: (subject: string) =>
      httpClient.request(`/api/v1/identity/users/${encodeURIComponent(subject)}/revoke-all`, { method: 'POST' }),
    onSuccess: async () => {
      void messageApi.success('该用户的全部刷新会话已撤销')
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '撤销失败') },
  })

  return (
    <>
      {contextHolder}
      <Card
        title="全局身份用户"
        extra={has('idp:identity-user:create') && <Button type="primary" onClick={() => setModalOpen(true)}>创建用户</Button>}
      >
        <PageState loading={usersQuery.isPending} error={usersQuery.error} empty={usersQuery.data?.length === 0} onRetry={() => { void usersQuery.refetch() }}>
          <Table<IdentityUser>
            rowKey="subject"
            dataSource={usersQuery.data ?? []}
            columns={[
              { title: '用户名', dataIndex: 'username' },
              { title: '显示名', dataIndex: 'displayName' },
              { title: '状态', dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> },
              { title: 'Token 版本', dataIndex: 'tokenVersion' },
              {
                title: '操作',
                render: (_: unknown, row: IdentityUser) => (
                  <Space>
                    {has('idp:identity-user:password-reset') && <Button size="small" onClick={() => resetPasswordMutation.mutate(row.subject)}>重置密码</Button>}
                    {has('idp:identity-user:revoke-all') && <Button size="small" danger onClick={() => revokeMutation.mutate(row.subject)}>撤销会话</Button>}
                  </Space>
                ),
              },
            ]}
          />
        </PageState>
      </Card>
      <Modal
        title="创建全局身份用户"
        open={modalOpen}
        confirmLoading={createMutation.isPending}
        onCancel={() => setModalOpen(false)}
        onOk={() => { void form.validateFields().then((values) => createMutation.mutate(values)) }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
