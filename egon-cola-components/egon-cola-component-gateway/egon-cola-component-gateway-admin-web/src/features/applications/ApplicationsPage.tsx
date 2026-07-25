import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { Application, Credential, IssuedCredential } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'

export const ApplicationsPage = () => {
  const { scope } = useScope()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:applications:write')
  const canWriteCredential = useCapability('gateway:credentials:write')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<Application>()
  const [application, setApplication] = useState<Application>()
  const [issued, setIssued] = useState<IssuedCredential>()
  const applications = useQuery({
    queryKey: ['applications', scope],
    queryFn: ({ signal }) => gatewayApi.applications(scope, signal),
  })
  const credentials = useQuery({
    queryKey: ['credentials', application?.id],
    queryFn: ({ signal }) => gatewayApi.credentials(application!.id, signal),
    enabled: Boolean(application),
  })
  const save = useMutation({
    mutationFn: (values: any) => editing
      ? gatewayApi.updateApplication(editing.id, {
          displayName: values.displayName,
          description: values.description,
          expectedRevision: editing.revision,
        })
      : gatewayApi.createApplication({ ...values, ...scope }),
    onSuccess: async () => {
      setEditing(undefined)
      form.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['applications', scope] })
      void message.success('Application 已保存')
    },
  })
  const createCredential = useMutation({
    mutationFn: () => gatewayApi.createCredential(application!.id),
    onSuccess: async (value) => {
      setIssued(value)
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
    },
  })
  const rotate = useMutation({
    mutationFn: ({ credential, overlapMinutes }: {
      credential: Credential
      overlapMinutes: number
    }) => gatewayApi.rotateCredential(application!.id, credential.id, overlapMinutes),
    onSuccess: async (value) => {
      setIssued(value)
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
    },
  })
  const revoke = useMutation({
    mutationFn: (credential: Credential) =>
      gatewayApi.revokeCredential(application!.id, credential.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
      void message.success('Credential 已吊销')
    },
  })

  if (applications.isLoading) return <LoadingBlock />
  if (applications.error) return <QueryFailure error={applications.error} />
  return (
    <section>
      <Space className="page-title" align="center">
        <Typography.Title level={2}>Application / Credential</Typography.Title>
        <Button
          type="primary"
          disabled={!canWrite}
          onClick={() => {
            setEditing({} as Application)
            form.resetFields()
          }}
        >
          新建 Application
        </Button>
      </Space>
      <Table<Application>
        rowKey="id"
        dataSource={applications.data}
        columns={[
          { title: 'Code', dataIndex: 'applicationCode' },
          { title: '名称', dataIndex: 'displayName' },
          { title: 'Env', dataIndex: 'env' },
          { title: 'Namespace', dataIndex: 'namespace' },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, row) => (
              <Space>
                <Button onClick={() => setApplication(row)}>Credential</Button>
                <Button
                  disabled={!canWrite}
                  onClick={() => {
                    setEditing(row)
                    form.setFieldsValue(row)
                  }}
                >
                  编辑
                </Button>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing?.id ? '编辑 Application' : '新建 Application'}
        open={Boolean(editing)}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          {!editing?.id && (
            <Form.Item name="applicationCode" label="Application Code" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          )}
          <Form.Item name="displayName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
      <Modal
        width={900}
        title={`Credential · ${application?.displayName ?? ''}`}
        open={Boolean(application)}
        onCancel={() => {
          setApplication(undefined)
          setIssued(undefined)
        }}
        footer={null}
      >
        {issued && (
          <Alert
            type="warning"
            showIcon
            message="Secret 只显示一次，请立即保存到 Secret 管理系统"
            description={<JsonPanel title="新 Credential" value={issued} />}
          />
        )}
        <Card
          extra={(
            <Button
              type="primary"
              disabled={!canWriteCredential}
              loading={createCredential.isPending}
              onClick={() => createCredential.mutate()}
            >
              签发 Credential
            </Button>
          )}
        >
          <Table<Credential>
            rowKey="id"
            loading={credentials.isLoading}
            dataSource={credentials.data}
            columns={[
              { title: 'Access Key', dataIndex: 'accessKey' },
              { title: '状态', dataIndex: 'status' },
              { title: '生效时间', dataIndex: 'validFrom' },
              { title: '失效时间', dataIndex: 'validUntil' },
              {
                title: '操作',
                render: (_, row) => (
                  <Space>
                    <Popconfirm
                      title="轮换 Credential"
                      description="旧 Credential 将保留 10 分钟重叠窗口。"
                      onConfirm={() => rotate.mutate({
                        credential: row,
                        overlapMinutes: 10,
                      })}
                    >
                      <Button disabled={!canWriteCredential || row.status === 'REVOKED'}>轮换</Button>
                    </Popconfirm>
                    <Popconfirm
                      title="确认吊销？"
                      description="吊销后该 Access Key 将立即失效。"
                      onConfirm={() => revoke.mutate(row)}
                    >
                      <Button danger disabled={!canWriteCredential || row.status === 'REVOKED'}>
                        吊销
                      </Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
          />
        </Card>
      </Modal>
    </section>
  )
}
