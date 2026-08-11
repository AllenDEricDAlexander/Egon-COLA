import { Button, Card, Form, Input, Modal, Table, message } from 'antd'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth, httpClient } from '../../auth/AuthContext'
import { usePermission, PageState } from '@egon-cola/admin-web-shared'
import type { OAuthClientView } from '../../api/types'

interface ClientFormValues {
  clientId: string; clientName: string; redirectUri: string; resourceUri: string
}

export const ClientListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<ClientFormValues>()
  const [messageApi, contextHolder] = message.useMessage()

  const clientsQuery = useQuery({
    queryKey: ['idp', 'clients'],
    queryFn: () => httpClient.request<OAuthClientView[]>('/api/v1/identity/clients'),
  })

  const createMutation = useMutation({
    mutationFn: (values: ClientFormValues) =>
      httpClient.request('/api/v1/identity/clients', {
        method: 'POST',
        body: JSON.stringify({
          clientId: values.clientId, clientName: values.clientName,
          accessTokenTtlSeconds: 900, refreshTokenTtlSeconds: 604800,
          redirectUris: [values.redirectUri], resourceUris: [values.resourceUri],
        }),
      }),
    onSuccess: async () => {
      setModalOpen(false); form.resetFields()
      void messageApi.success('客户端已创建')
      await queryClient.invalidateQueries({ queryKey: ['idp', 'clients'] })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '创建失败') },
  })

  return (
    <>
      {contextHolder}
      <Card
        title="OAuth 公共客户端"
        extra={has('idp:oauth-client:create') && <Button type="primary" onClick={() => setModalOpen(true)}>创建客户端</Button>}
      >
        <PageState loading={clientsQuery.isPending} error={clientsQuery.error} empty={clientsQuery.data?.length === 0} onRetry={() => { void clientsQuery.refetch() }}>
          <Table<OAuthClientView> rowKey="clientId" dataSource={clientsQuery.data ?? []} columns={[
            { title: 'Client ID', dataIndex: 'clientId' },
            { title: '名称', dataIndex: 'clientName' },
            { title: '状态', dataIndex: 'status' },
            { title: 'PKCE', dataIndex: 'pkceRequired', render: (v: boolean) => v ? 'S256' : '否' },
            { title: '回调地址', dataIndex: 'redirectUris', render: (v: string[]) => v.join(', ') },
            { title: 'Resource URI', dataIndex: 'resourceUris', render: (v: string[]) => v.join(', ') },
          ]} />
        </PageState>
      </Card>
      <Modal
        title="创建 OAuth 公共客户端" open={modalOpen} confirmLoading={createMutation.isPending}
        onCancel={() => setModalOpen(false)}
        onOk={() => { void form.validateFields().then((values) => createMutation.mutate(values)) }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="clientId" label="Client ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="clientName" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="redirectUri" label="精确回调地址" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="resourceUri" label="Resource URI" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
