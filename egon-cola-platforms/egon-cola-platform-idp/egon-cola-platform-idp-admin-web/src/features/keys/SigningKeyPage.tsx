import {Button, Card, Form, Input, message, Modal, Space, Table, Tag, Typography} from 'antd'
import {CheckCircleOutlined, PlusOutlined, ReloadOutlined, StopOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import type {SigningKeyVO} from '../../api/types'

const STATUS_COLORS: Record<string, string> = {
    PUBLISHED: 'blue',
    ACTIVE: 'green',
    RETIRED: 'orange',
}

export const SigningKeyPage = () => {
    const auth = useAuth()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [publishOpen, setPublishOpen] = useState(false)
    const [form] = Form.useForm()
    const [messageApi, contextHolder] = message.useMessage()

    const keysQuery = useQuery({
        queryKey: ['idp', 'signing-keys'],
        queryFn: () => httpClient.request<SigningKeyVO[]>('/api/v1/identity/signing-keys'),
    })

    const publishMutation = useMutation({
        mutationFn: (v: { kid: string; encryptedPrivateKey: string; publicJwk: string }) =>
            httpClient.request<SigningKeyVO>('/api/v1/identity/signing-keys', {
                method: 'POST',
                body: JSON.stringify(v),
            }),
        onSuccess: async () => {
            setPublishOpen(false)
            form.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'signing-keys']})
            messageApi.success('签名密钥已预发布')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '发布失败')
        },
    })

    const activateMutation = useMutation({
        mutationFn: ({kid, version}: { kid: string; version: number }) =>
            httpClient.request<SigningKeyVO>(
                `/api/v1/identity/signing-keys/${encodeURIComponent(kid)}/activate?expectedVersion=${version}`,
                {method: 'POST'},
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: ['idp', 'signing-keys']})
            messageApi.success('密钥已激活')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '激活失败')
        },
    })

    const retireMutation = useMutation({
        mutationFn: ({kid, version}: { kid: string; version: number }) =>
            httpClient.request<SigningKeyVO>(
                `/api/v1/identity/signing-keys/${encodeURIComponent(kid)}/retire?expectedVersion=${version}`,
                {method: 'POST'},
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: ['idp', 'signing-keys']})
            messageApi.success('密钥已退役')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '退役失败')
        },
  })

  return (
      <>
          {contextHolder}
          <Card
              title="签名密钥"
              extra={
                  <Space>
                      <Button icon={<ReloadOutlined/>} onClick={() => {
                          void keysQuery.refetch()
                      }}>刷新</Button>
                      {has('idp:signing-key:publish') && (
                          <Button type="primary" icon={<PlusOutlined/>}
                                  onClick={() => setPublishOpen(true)}>预发布密钥</Button>
                      )}
                  </Space>
              }
          >
              <Typography.Paragraph type="secondary">
                  私钥材料永不返回浏览器。公钥 JWK 供外部验证 IdP 签名用。密钥生命周期：预发布 → 激活 → 退役。
              </Typography.Paragraph>
              <PageState
                  loading={keysQuery.isPending}
                  error={keysQuery.error}
                  empty={keysQuery.data?.length === 0}
                  emptyDescription="暂无签名密钥，请预发布一个"
                  onRetry={() => {
                      void keysQuery.refetch()
                  }}
              >
                  <Table<SigningKeyVO>
                      rowKey="kid"
                      dataSource={keysQuery.data ?? []}
                      columns={[
                          {title: 'KID', dataIndex: 'kid', ellipsis: true},
                          {title: '算法', dataIndex: 'algorithm'},
                          {
                              title: '状态',
                              dataIndex: 'status',
                              render: (v: string) => <Tag color={STATUS_COLORS[v] ?? 'default'}>{v}</Tag>
                          },
                          {
                              title: '当前服务', dataIndex: 'runtimeServing',
                              render: (v: boolean) => v ? <Tag color="green">是</Tag> : <Tag>否</Tag>,
                          },
                          {title: '激活时间', dataIndex: 'activatedAt', render: (v?: string) => v ?? '-'},
                          {title: '退役时间', dataIndex: 'retiredAt', render: (v?: string) => v ?? '-'},
                          {title: '版本', dataIndex: 'version', width: 80},
                          {title: '创建时间', dataIndex: 'createdAt'},
                          {
                              title: '操作', width: 160,
                              render: (_: unknown, row: SigningKeyVO) => (
                                  <Space size="small">
                                      {has('idp:signing-key:activate') && row.status === 'PUBLISHED' && (
                                          <Button size="small" icon={<CheckCircleOutlined/>} type="primary"
                                                  onClick={() => activateMutation.mutate({
                                                      kid: row.kid,
                                                      version: row.version
                                                  })}>激活</Button>
                                      )}
                                      {has('idp:signing-key:retire') && row.status === 'ACTIVE' && (
                                          <Button size="small" icon={<StopOutlined/>} danger
                                                  onClick={() => retireMutation.mutate({
                                                      kid: row.kid,
                                                      version: row.version
                                                  })}>退役</Button>
                                      )}
                                  </Space>
                              ),
                          },
                      ]}
                  />
              </PageState>
          </Card>

          {/* Publish Modal */}
          <Modal
              title="预发布签名密钥"
              open={publishOpen}
              width={560}
              confirmLoading={publishMutation.isPending}
              onCancel={() => {
                  setPublishOpen(false);
                  form.resetFields()
              }}
              onOk={() => {
                  void form.validateFields().then((v) => publishMutation.mutate(v))
              }}
              destroyOnClose
          >
              <Form form={form} layout="vertical" preserve={false}>
                  <Form.Item name="kid" label="KID" rules={[{required: true}]}>
                      <Input placeholder="signing-key-2025"/>
                  </Form.Item>
                  <Form.Item name="encryptedPrivateKey" label="加密私钥" rules={[{required: true}]}>
                      <Input.TextArea rows={4} placeholder="加密后的 PEM 格式私钥"/>
                  </Form.Item>
                  <Form.Item name="publicJwk" label="公开 JWK JSON" rules={[{required: true}]}>
                      <Input.TextArea rows={4} placeholder='{"kty":"RSA","n":"...","e":"AQAB"}'/>
                  </Form.Item>
              </Form>
          </Modal>
      </>
  )
}
