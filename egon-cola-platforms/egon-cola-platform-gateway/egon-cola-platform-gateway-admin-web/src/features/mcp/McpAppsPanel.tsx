import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Upload,
  type UploadFile,
  message,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpAppArtifact, McpCapabilityDraft } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { parseStringList, validateResourceUri } from './mcpValidation'
import { useMcpCapabilityCollection } from './useMcpCapabilityCollection'

type ArtifactForm = {
  appCode: string
  version: string
  displayName: string
  resourceUri: string
  mimeType: string
  contentSecurityPolicy: string
  permissions: string
  allowedOrigins?: string
  changeReason: string
}

type BindingForm = {
  name: string
  appArtifactId: string
  allowedTools?: string[]
  enabled: boolean
  changeReason: string
}

export const McpAppsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const canWrite = useCapability('gateway:mcp:write')
  const queryClient = useQueryClient()
  const bindings = useMcpCapabilityCollection(
    'app-bindings',
    serverId,
    gatewayGroupId,
    draftRevision,
  )
  const artifacts = useQuery({
    queryKey: ['mcp-app-artifacts', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpAppArtifacts(gatewayGroupId, signal),
  })
  const tools = useQuery({
    queryKey: ['mcp-tool-references', gatewayGroupId, serverId],
    queryFn: ({ signal }) => gatewayApi.mcpToolReferences(gatewayGroupId, serverId, signal),
  })
  const [artifactForm] = Form.useForm<ArtifactForm>()
  const [bindingForm] = Form.useForm<BindingForm>()
  const [artifactOpen, setArtifactOpen] = useState(false)
  const [bindingOpen, setBindingOpen] = useState(false)
  const [editingBinding, setEditingBinding] = useState<McpCapabilityDraft>()
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [previewHtml, setPreviewHtml] = useState<string>()
  const [selectedArtifact, setSelectedArtifact] = useState<McpAppArtifact>()

  const upload = useMutation({
    mutationFn: (values: ArtifactForm) => {
      const artifact = fileList[0]?.originFileObj
      if (!artifact) throw new Error('请选择 App HTML Artifact')
      return gatewayApi.uploadMcpAppArtifact({
        ...values,
        gatewayGroupId,
        resourceUri: validateResourceUri(values.resourceUri),
        permissions: parseStringList(values.permissions),
        allowedOrigins: parseStringList(values.allowedOrigins),
        expectedRevision: 0,
        expectedDraftRevision: draftRevision,
        artifact,
      })
    },
    onSuccess: async () => {
      setArtifactOpen(false)
      setFileList([])
      setPreviewHtml(undefined)
      artifactForm.resetFields()
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-app-artifacts', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
      void message.success('MCP App Artifact 已完成安全校验并登记')
    },
  })
  const revoke = useMutation({
    mutationFn: (artifact: McpAppArtifact) => gatewayApi.revokeMcpAppArtifact(artifact.id, {
      gatewayGroupId,
      expectedRevision: 0,
      expectedDraftRevision: draftRevision,
      changeReason: 'Revoke MCP App artifact from Admin Web',
    }),
    onSuccess: async () => {
      setSelectedArtifact(undefined)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['mcp-app-artifacts', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      ])
    },
  })

  const openArtifact = () => {
    upload.reset()
    setFileList([])
    setPreviewHtml(undefined)
    artifactForm.setFieldsValue({
      appCode: '',
      version: '1.0.0',
      displayName: '',
      resourceUri: '',
      mimeType: 'text/html',
      contentSecurityPolicy: "default-src 'none'; script-src 'self'; style-src 'self'",
      permissions: 'ui:render',
      allowedOrigins: '',
      changeReason: '',
    })
    setArtifactOpen(true)
  }

  const openBinding = (binding?: McpCapabilityDraft) => {
    const content = binding?.content ?? {}
    setEditingBinding(binding)
    bindingForm.setFieldsValue(binding ? {
      name: binding.name,
      appArtifactId: typeof content.appArtifactId === 'string' ? content.appArtifactId : '',
      allowedTools: Array.isArray(content.allowedTools) ? content.allowedTools as string[] : [],
      enabled: binding.enabled,
      changeReason: '',
    } : {
      name: '',
      appArtifactId: '',
      allowedTools: [],
      enabled: true,
      changeReason: '',
    })
    setBindingOpen(true)
  }

  const saveBinding = (values: BindingForm) => bindings.save.mutate({
    editing: editingBinding,
    name: values.name,
    enabled: values.enabled,
    changeReason: values.changeReason,
    content: {
      appArtifactId: values.appArtifactId,
      allowedTools: values.allowedTools ?? [],
    },
  }, {
    onSuccess: () => {
      setBindingOpen(false)
      setEditingBinding(undefined)
    },
  })

  return (
    <section>
      <Tabs items={[
        {
          key: 'artifacts',
          label: 'Immutable Artifacts',
          children: (
            <section>
              <Alert
                type="info"
                showIcon
                title="App Artifact 上传后按 SHA-256、CSP、权限清单和允许来源做不可变登记。"
                style={{ marginBottom: 16 }}
              />
              <Button type="primary" disabled={!canWrite} onClick={openArtifact} style={{ marginBottom: 16 }}>
                上传 App Artifact
              </Button>
              {artifacts.error && <QueryFailure error={artifacts.error} />}
              <Table<McpAppArtifact>
                rowKey="id"
                loading={artifacts.isLoading}
                dataSource={artifacts.data ?? []}
                columns={[
                  { title: 'App', dataIndex: 'appCode' },
                  { title: 'Version', dataIndex: 'version' },
                  { title: 'Resource URI', dataIndex: 'resourceUri' },
                  { title: 'SHA-256', render: (_, row) => `${row.sha256.slice(0, 12)}…` },
                  { title: 'Bytes', dataIndex: 'sizeBytes' },
                  {
                    title: '操作',
                    render: (_, row) => <Space>
                      <Button onClick={() => setSelectedArtifact(row)}>安全报告</Button>
                      <Popconfirm title="确认撤销不可变 Artifact？" onConfirm={() => revoke.mutate(row)}>
                        <Button danger disabled={!canWrite}>撤销</Button>
                      </Popconfirm>
                    </Space>,
                  },
                ]}
              />
            </section>
          ),
        },
        {
          key: 'bindings',
          label: 'App Bindings',
          children: (
            <section>
              <Button
                type="primary"
                disabled={!canWrite}
                onClick={() => openBinding()}
                style={{ marginBottom: 16 }}
              >新增 App Binding</Button>
              {bindings.query.error && <QueryFailure error={bindings.query.error} />}
              <Table<McpCapabilityDraft>
                rowKey="id"
                loading={bindings.query.isLoading}
                dataSource={bindings.query.data ?? []}
                columns={[
                  { title: 'Name', dataIndex: 'name' },
                  { title: 'Artifact', render: (_, row) => String(row.content.appArtifactId ?? '-') },
                  { title: 'Revision', dataIndex: 'revision' },
                  {
                    title: '操作',
                    render: (_, row) => <Space>
                      <Button disabled={!canWrite} onClick={() => openBinding(row)}>编辑</Button>
                      <Popconfirm title="确认删除 App Binding？" onConfirm={() => bindings.remove.mutate(row)}>
                        <Button danger disabled={!canWrite}>删除</Button>
                      </Popconfirm>
                    </Space>,
                  },
                ]}
              />
            </section>
          ),
        },
      ]} />
      <Modal
        title="上传 MCP App Artifact"
        open={artifactOpen}
        width={800}
        onCancel={() => setArtifactOpen(false)}
        onOk={() => artifactForm.submit()}
        confirmLoading={upload.isPending}
        destroyOnHidden
      >
        {upload.error && <QueryFailure error={upload.error} />}
        <Form form={artifactForm} layout="vertical" onFinish={(values) => upload.mutate(values)}>
          <Form.Item name="appCode" label="App Code" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="version" label="Version" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="resourceUri" label="Resource URI" rules={[{ required: true }]}>
            <Input placeholder="ui://server-code/app-code" />
          </Form.Item>
          <Form.Item name="mimeType" label="MIME Type" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="contentSecurityPolicy" label="Content Security Policy" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="permissions" label="Permission Manifest（逗号分隔）" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="allowedOrigins" label="Allowed Origins（逗号分隔）"><Input /></Form.Item>
          <Form.Item label="HTML Artifact" required>
            <Upload
              accept="text/html,.html"
              maxCount={1}
              beforeUpload={() => false}
              fileList={fileList}
              onChange={({ fileList: next }) => {
                setFileList(next.slice(-1))
                const file = next.at(-1)?.originFileObj
                if (file) void file.text().then(setPreviewHtml)
              }}
            >
              <Button>选择 HTML</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
        </Form>
        {previewHtml && (
          <Card title="Sandbox Preview">
            <iframe
              title="MCP App sandbox preview"
              sandbox=""
              srcDoc={previewHtml}
              style={{ width: '100%', minHeight: 260, border: 0 }}
            />
          </Card>
        )}
      </Modal>
      <Modal
        title={editingBinding ? '编辑 App Binding' : '新增 App Binding'}
        open={bindingOpen}
        onCancel={() => setBindingOpen(false)}
        onOk={() => bindingForm.submit()}
        confirmLoading={bindings.save.isPending}
        destroyOnHidden
      >
        {bindings.save.error && <QueryFailure error={bindings.save.error} />}
        <Form form={bindingForm} layout="vertical" onFinish={saveBinding}>
          <Form.Item name="name" label="App Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="appArtifactId" label="Artifact" rules={[{ required: true }]}>
            <Select options={(artifacts.data ?? []).map((artifact) => ({
              value: artifact.id,
              label: `${artifact.appCode}@${artifact.version} · ${artifact.sha256.slice(0, 12)}`,
            }))} />
          </Form.Item>
          <Form.Item name="allowedTools" label="Allowed Tools">
            <Select
              mode="multiple"
              options={(tools.data ?? [])
                .filter((tool) => tool.enabled)
                .map((tool) => ({ value: tool.name, label: tool.name }))}
            />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="App Artifact 安全报告"
        open={Boolean(selectedArtifact)}
        onCancel={() => setSelectedArtifact(undefined)}
        footer={null}
      >
        {selectedArtifact && (
          <Descriptions column={1}>
            <Descriptions.Item label="SHA-256">{selectedArtifact.sha256}</Descriptions.Item>
            <Descriptions.Item label="CSP">{selectedArtifact.contentSecurityPolicy}</Descriptions.Item>
            <Descriptions.Item label="Permissions">{selectedArtifact.permissions.join(', ')}</Descriptions.Item>
            <Descriptions.Item label="Allowed Origins">
              {selectedArtifact.allowedOrigins.join(', ') || 'none'}
            </Descriptions.Item>
            <Descriptions.Item label="Artifact Reference">
              {selectedArtifact.artifactReference}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </section>
  )
}
