import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {GatewayApiError} from '../../api/client'
import {
  embeddingRoutesRejected,
  llmApi,
  llmCapabilityValues,
  llmDeploymentValues,
  llmModelKindValues,
  llmProtocolValues,
  type LlmChannel,
  type LlmChannelCommand,
  type LlmModel,
  type LlmModelCommand,
  type LlmModelRoute,
} from '../../api/llm'
import {useCapability} from '../../app/capabilities'
import {EmptyBlock, LoadingBlock, QueryFailure} from '../../components/QueryState'

const isForbidden = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 403
const isConflict = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 409

const keyPattern = /^[a-z0-9][a-z0-9-]{0,63}$/

type ChannelForm = {
  key: string
  name: string
  deployment: string
  protocol: string
  baseUrl: string
  secretRef: string
  enabled: boolean
  connectTimeoutMs: number
  headerTimeoutMs: number
  idleTimeoutMs: number
  totalTimeoutMs: number
  maxConcurrent: number
}

type ModelForm = {
  key: string
  name: string
  kind: string
  protocols: string[]
  enabled: boolean
  dimensions: number | null
  embeddingSpaceId: string
  allowedSubjects: string[]
  routes: LlmModelRoute[]
}

const channelToForm = (channel?: LlmChannel): ChannelForm => ({
  key: channel?.key ?? '',
  name: channel?.name ?? '',
  deployment: channel?.deployment ?? 'LOCAL',
  protocol: channel?.protocol ?? 'OPENAI_CHAT',
  baseUrl: channel?.baseUrl ?? '',
  secretRef: channel?.secretRef ?? '',
  enabled: channel?.enabled ?? true,
  connectTimeoutMs: channel?.connectTimeoutMs ?? 3_000,
  headerTimeoutMs: channel?.headerTimeoutMs ?? 30_000,
  idleTimeoutMs: channel?.idleTimeoutMs ?? 30_000,
  totalTimeoutMs: channel?.totalTimeoutMs ?? 120_000,
  maxConcurrent: channel?.maxConcurrent ?? 16,
})

const modelToForm = (model?: LlmModel): ModelForm => ({
  key: model?.key ?? '',
  name: model?.name ?? '',
  kind: model?.kind ?? 'CHAT',
  protocols: model?.protocols ?? [],
  enabled: model?.enabled ?? true,
  dimensions: model?.dimensions ?? null,
  embeddingSpaceId: model?.embeddingSpaceId ?? '',
  allowedSubjects: model?.allowedSubjects ?? [],
  routes: model?.routes ?? [],
})

// 模型网关配置页（§12.1「模型管理」）：只读需要 yuheng:llm:read，写操作额外需要 llm:write。
// secretRef 只是部署引用名，页面上任何分支都不会显示或回显密钥解析值。
export const LlmConfigurationPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const canWrite = useCapability('yuheng:llm:write')
  const [channelDraft, setChannelDraft] = useState<{open: boolean; original?: LlmChannel}>({open: false})
  const [modelDraft, setModelDraft] = useState<{open: boolean; original?: LlmModel}>({open: false})
  const [channelForm] = Form.useForm<ChannelForm>()
  const [modelForm] = Form.useForm<ModelForm>()
  const [embeddingRouteRejected, setEmbeddingRouteRejected] = useState(false)

  const channels = useQuery({
    queryKey: ['llm', 'channels'],
    queryFn: ({signal}) => llmApi.channels(1, 20, signal),
  })
  const models = useQuery({
    queryKey: ['llm', 'models'],
    queryFn: ({signal}) => llmApi.models(1, 20, signal),
  })

  const refreshConfig = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey: ['llm', 'channels']}),
      queryClient.invalidateQueries({queryKey: ['llm', 'models']}),
    ])
  }
  // 403 与登出一致：清除已读取的配置投影，不留可继续浏览的快照。
  const clearSensitiveCache = () => queryClient.removeQueries({queryKey: ['llm']})

  const saveChannel = useMutation({
    mutationFn: ({values, original}: {values: ChannelForm; original?: LlmChannel}) => {
      const command: LlmChannelCommand = {
        key: values.key.trim(),
        name: values.name.trim(),
        deployment: values.deployment,
        protocol: values.protocol,
        baseUrl: values.baseUrl.trim(),
        // 空串按合同的“LOCAL 且无认证”表达为 null，密钥值从不出现在此处。
        secretRef: values.secretRef?.trim() ? values.secretRef.trim() : null,
        enabled: values.enabled,
        connectTimeoutMs: values.connectTimeoutMs,
        headerTimeoutMs: values.headerTimeoutMs,
        idleTimeoutMs: values.idleTimeoutMs,
        totalTimeoutMs: values.totalTimeoutMs,
        maxConcurrent: values.maxConcurrent,
        expectedRevision: original?.revision ?? 0,
      }
      return llmApi.replaceChannel(command.key, command)
    },
    retry: false,
    onSuccess: async () => {
      setChannelDraft({open: false})
      await refreshConfig()
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const toggleChannel = useMutation({
    mutationFn: ({channel, enabled}: {channel: LlmChannel; enabled: boolean}) =>
      llmApi.replaceChannel(channel.key, {
        key: channel.key,
        name: channel.name,
        deployment: channel.deployment,
        protocol: channel.protocol,
        baseUrl: channel.baseUrl,
        secretRef: channel.secretRef,
        enabled,
        connectTimeoutMs: channel.connectTimeoutMs,
        headerTimeoutMs: channel.headerTimeoutMs,
        idleTimeoutMs: channel.idleTimeoutMs,
        totalTimeoutMs: channel.totalTimeoutMs,
        maxConcurrent: channel.maxConcurrent,
        expectedRevision: channel.revision,
      }),
    retry: false,
    onSuccess: refreshConfig,
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const saveModel = useMutation({
    mutationFn: ({values, original}: {values: ModelForm; original?: LlmModel}) => {
      const command: LlmModelCommand = {
        key: values.key.trim(),
        name: values.name.trim(),
        kind: values.kind,
        protocols: values.protocols as LlmModelCommand['protocols'],
        enabled: values.enabled,
        // CHAT 两者的 null 由服务端复核，前端不发送残留值，避免“看起来还能改”的错觉。
        dimensions: values.kind === 'EMBEDDING' ? values.dimensions : null,
        embeddingSpaceId: values.kind === 'EMBEDDING' && values.embeddingSpaceId?.trim()
          ? values.embeddingSpaceId.trim()
          : null,
        allowedSubjects: values.allowedSubjects ?? [],
        routes: values.routes ?? [],
        expectedRevision: original?.revision ?? 0,
      }
      return llmApi.replaceModel(command.key, command)
    },
    retry: false,
    onSuccess: async () => {
      setModelDraft({open: false})
      setEmbeddingRouteRejected(false)
      await refreshConfig()
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })

  const submitModel = (values: ModelForm) => {
    // 嵌入通道必须全部本地：先在前端拒绝，再让服务端做权威复核，不做云嵌入回退。
    if (embeddingRoutesRejected(values.kind, values.routes ?? [], channels.data?.items ?? [])) {
      setEmbeddingRouteRejected(true)
      return
    }
    setEmbeddingRouteRejected(false)
    saveModel.mutate({values, original: modelDraft.original})
  }

  const listError = channels.error ?? models.error
  if (channels.isLoading || models.isLoading) return <LoadingBlock />
  if (!channels.data || !models.data) {
    return <QueryFailure error={listError} retry={() => {
      if (isForbidden(listError)) clearSensitiveCache()
      void channels.refetch()
      void models.refetch()
    }} />
  }

  const tab = searchParams.get('tab') ?? 'channels'
  const channelError = saveChannel.error ?? toggleChannel.error
  const editingEmbedding = modelDraft.original?.kind === 'EMBEDDING'

  return (
    <Space direction="vertical" size="middle" style={{width: '100%'}}>
      <Typography.Title level={2} style={{marginBottom: 0}}>
        模型网关配置
      </Typography.Title>
      <Alert
        type="info"
        showIcon
        message="渠道与模型 alias 是独立字典"
        description="页面只显示部署引用名，密钥解析值永不返回前端；配置变更的并发只由 expectedRevision 控制。"
      />
      <Tabs
        activeKey={tab}
        onChange={(next) => setSearchParams({tab: next})}
        items={[
          {
            key: 'channels',
            label: '渠道',
            children: (
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
                <Space wrap>
                  <Button
                    type="primary"
                    disabled={!canWrite || saveChannel.isPending}
                    title={canWrite ? undefined : '缺少 yuheng:llm:write 能力'}
                    onClick={() => {
                      setChannelDraft({open: true})
                      channelForm.setFieldsValue(channelToForm())
                    }}
                  >
                    新建渠道
                  </Button>
                  <Button onClick={() => void channels.refetch()}>刷新渠道</Button>
                </Space>
                {channelError && (
                  <Alert
                    type="error"
                    showIcon
                    message={channelError instanceof Error ? channelError.message : '保存渠道失败'}
                    description={isConflict(channelError)
                      ? '渠道版本已被他人推进：你的输入已保留，请刷新比较后再提交。'
                      : undefined}
                    action={<Button size="small" onClick={() => void channels.refetch()}>刷新比较</Button>}
                  />
                )}
                <Table<LlmChannel>
                  rowKey="key"
                  dataSource={channels.data.items}
                  pagination={false}
                  locale={{emptyText: <EmptyBlock description="暂无渠道配置" />}}
                  columns={[
                    {title: '渠道键', dataIndex: 'key'},
                    {title: '名称', dataIndex: 'name'},
                    {
                      title: '部署',
                      dataIndex: 'deployment',
                      render: (value: string) => <Tag>{value}</Tag>,
                    },
                    {title: '协议', dataIndex: 'protocol'},
                    {title: '根地址', dataIndex: 'baseUrl'},
                    {
                      title: '密钥引用',
                      dataIndex: 'secretRef',
                      render: (value: string | null) => value ?? '无认证',
                    },
                    {
                      title: '超时（连接/头/空闲/总）',
                      render: (_, record) =>
                        `${record.connectTimeoutMs}/${record.headerTimeoutMs}/${record.idleTimeoutMs}/${record.totalTimeoutMs} ms`,
                    },
                    {title: '并发', dataIndex: 'maxConcurrent'},
                    {
                      title: '状态',
                      render: (_, record) => (
                        <Tag>{record.enabled ? '已启用' : '已停用'}</Tag>
                      ),
                    },
                    {title: '版本', dataIndex: 'revision'},
                    {
                      title: '操作',
                      render: (_, record) => (
                        <Space wrap>
                          <Button
                            size="small"
                            disabled={!canWrite}
                            onClick={() => {
                              setChannelDraft({open: true, original: record})
                              channelForm.setFieldsValue(channelToForm(record))
                            }}
                          >
                            编辑渠道
                          </Button>
                          <Button
                            size="small"
                            disabled={!canWrite || toggleChannel.isPending}
                            onClick={() => toggleChannel.mutate({
                              channel: record,
                              enabled: !record.enabled,
                            })}
                          >
                            {record.enabled ? '停用渠道' : '启用渠道'}
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Space>
            ),
          },
          {
            key: 'models',
            label: '模型映射',
            children: (
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
                <Space wrap>
                  <Button
                    type="primary"
                    disabled={!canWrite || saveModel.isPending}
                    onClick={() => {
                      setModelDraft({open: true})
                      setEmbeddingRouteRejected(false)
                      modelForm.setFieldsValue(modelToForm())
                    }}
                  >
                    新建模型 alias
                  </Button>
                  <Button onClick={() => void models.refetch()}>刷新模型</Button>
                </Space>
                {saveModel.isError && (
                  <Alert
                    type="error"
                    showIcon
                    message={saveModel.error instanceof Error ? saveModel.error.message : '保存模型失败'}
                    description={isConflict(saveModel.error)
                      ? '模型版本已被他人推进：你的输入已保留，请刷新比较后再提交。'
                      : undefined}
                    action={<Button size="small" onClick={() => void models.refetch()}>刷新比较</Button>}
                  />
                )}
                <Table<LlmModel>
                  rowKey="key"
                  dataSource={models.data.items}
                  pagination={false}
                  locale={{emptyText: <EmptyBlock description="暂无模型 alias" />}}
                  expandable={{
                    expandedRowRender: (record) => (
                      <Table<LlmModelRoute>
                        size="small"
                        rowKey={(route) => `${route.channelKey}:${route.upstreamModel}`}
                        dataSource={record.routes}
                        pagination={false}
                        columns={[
                          {title: '渠道', dataIndex: 'channelKey'},
                          {title: '上游模型', dataIndex: 'upstreamModel'},
                          {
                            title: '能力',
                            dataIndex: 'capabilities',
                            render: (values: string[]) => values.map((value) => <Tag key={value}>{value}</Tag>),
                          },
                          {title: '优先级', dataIndex: 'priority'},
                          {title: '权重', dataIndex: 'weight'},
                        ]}
                      />
                    ),
                  }}
                  columns={[
                    {title: '模型 alias', dataIndex: 'key'},
                    {title: '名称', dataIndex: 'name'},
                    {
                      title: '类型',
                      dataIndex: 'kind',
                      render: (value: string) => <Tag>{value}</Tag>,
                    },
                    {
                      title: '入口协议',
                      dataIndex: 'protocols',
                      render: (values: string[]) => values.map((value) => <Tag key={value}>{value}</Tag>),
                    },
                    {
                      title: '嵌入空间',
                      render: (_, record) => (record.kind === 'EMBEDDING'
                        ? `${record.embeddingSpaceId ?? '—'} · ${record.dimensions ?? '—'} 维（不可原地更换）`
                        : '—'),
                    },
                    {
                      title: '可调用服务',
                      dataIndex: 'allowedSubjects',
                      render: (values: string[]) => values.join('、') || '—',
                    },
                    {
                      title: '路由数',
                      render: (_, record) => record.routes.length,
                    },
                    {
                      title: '状态',
                      render: (_, record) => <Tag>{record.enabled ? '已启用' : '已停用'}</Tag>,
                    },
                    {title: '版本', dataIndex: 'revision'},
                    {
                      title: '操作',
                      render: (_, record) => (
                        <Button
                          size="small"
                          disabled={!canWrite}
                          onClick={() => {
                            setModelDraft({open: true, original: record})
                            setEmbeddingRouteRejected(false)
                            modelForm.setFieldsValue(modelToForm(record))
                          }}
                        >
                          编辑模型
                        </Button>
                      ),
                    },
                  ]}
                />
              </Space>
            ),
          },
        ]}
      />

      <Drawer
        open={channelDraft.open}
        title={channelDraft.original ? `编辑渠道 ${channelDraft.original.key}` : '新建渠道'}
        width={600}
        onClose={() => setChannelDraft({open: false})}
        extra={(
          <Button
            type="primary"
            loading={saveChannel.isPending}
            disabled={saveChannel.isPending}
            onClick={() => channelForm.submit()}
          >
            保存渠道
          </Button>
        )}
      >
        <Form<ChannelForm>
          form={channelForm}
          layout="vertical"
          disabled={!canWrite || saveChannel.isPending}
          initialValues={channelToForm(channelDraft.original)}
          onFinish={(values) => saveChannel.mutate({values, original: channelDraft.original})}
        >
          <Form.Item
            name="key"
            label="渠道键"
            extra="1–64 位小写字母、数字或连字符；创建后不可改名。"
            rules={[
              {required: true, message: '请填写渠道键'},
              {pattern: keyPattern, message: '只允许 1–64 位小写字母、数字或连字符'},
            ]}
          >
            <Input aria-label="渠道键输入" disabled={Boolean(channelDraft.original)} />
          </Form.Item>
          <Form.Item
            name="name"
            label="展示名称"
            rules={[{required: true, message: '请填写名称'}, {max: 128}]}
          >
            <Input aria-label="渠道名称输入" />
          </Form.Item>
          <Form.Item name="deployment" label="部署" rules={[{required: true}]}>
            <Select
              aria-label="部署选择"
              options={llmDeploymentValues.map((value) => ({
                value,
                label: value === 'LOCAL' ? 'LOCAL（不出域）' : 'CLOUD（可能出域）',
              }))}
            />
          </Form.Item>
          <Form.Item name="protocol" label="协议" rules={[{required: true}]}>
            <Select
              aria-label="协议选择"
              options={llmProtocolValues.map((value) => ({value, label: value}))}
            />
          </Form.Item>
          <Form.Item
            name="baseUrl"
            label="API 根地址"
            extra="必须是绝对 http(s) 地址；服务端另做白名单与 SSRF 校验。"
            rules={[
              {required: true, message: '请填写 API 根地址'},
              {type: 'url', message: '必须是合法绝对地址'},
              {pattern: /^https?:\/\//i, message: '仅支持 http(s) 绝对地址'},
            ]}
          >
            <Input aria-label="渠道根地址输入" />
          </Form.Item>
          <Form.Item
            name="secretRef"
            label="密钥引用名"
            extra="这里只填部署侧的引用名（例如 llm/local-main），绝不粘贴密钥值；留空表示 LOCAL 无认证。"
            rules={[
              {max: 128},
              {
                validator: (_, value: string) => {
                  const deployment = channelForm.getFieldValue('deployment')
                  if (deployment === 'CLOUD' && !value?.trim()) {
                    return Promise.reject(new Error('CLOUD 渠道必须提供密钥引用名'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input aria-label="密钥引用名输入" autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="connectTimeoutMs"
            label="连接超时（ms）"
            rules={[{required: true}, {type: 'integer', min: 1, max: 10_000, message: '1–10000'}]}
          >
            <InputNumber aria-label="连接超时输入" min={1} max={10_000} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item
            name="headerTimeoutMs"
            label="响应头超时（ms）"
            rules={[{required: true}, {type: 'integer', min: 1, max: 120_000, message: '1–120000'}]}
          >
            <InputNumber aria-label="响应头超时输入" min={1} max={120_000} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item
            name="idleTimeoutMs"
            label="流空闲超时（ms）"
            rules={[{required: true}, {type: 'integer', min: 1, max: 120_000, message: '1–120000'}]}
          >
            <InputNumber aria-label="空闲超时输入" min={1} max={120_000} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item
            name="totalTimeoutMs"
            label="整体截止（ms）"
            extra="含重试；关系约束由服务端复核。"
            rules={[{required: true}, {type: 'integer', min: 1, max: 600_000, message: '1–600000'}]}
          >
            <InputNumber aria-label="整体截止输入" min={1} max={600_000} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item
            name="maxConcurrent"
            label="单实例最大并发"
            extra="仅约束单 engine/channel，不是全局配额。"
            rules={[{required: true}, {type: 'integer', min: 1, max: 256, message: '1–256'}]}
          >
            <InputNumber aria-label="最大并发输入" min={1} max={256} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch aria-label="渠道启用开关" />
          </Form.Item>
          {channelDraft.original && (
            <Descriptions size="small" column={1} items={[{
              key: 'revision',
              label: '当前版本',
              children: channelDraft.original.revision,
            }]} />
          )}
        </Form>
      </Drawer>

      <Drawer
        open={modelDraft.open}
        title={modelDraft.original ? `编辑模型 ${modelDraft.original.key}` : '新建模型 alias'}
        width={720}
        onClose={() => setModelDraft({open: false})}
        extra={(
          <Button
            type="primary"
            loading={saveModel.isPending}
            disabled={saveModel.isPending}
            onClick={() => modelForm.submit()}
          >
            保存模型
          </Button>
        )}
      >
        <Form<ModelForm>
          form={modelForm}
          layout="vertical"
          disabled={!canWrite || saveModel.isPending}
          initialValues={modelToForm(modelDraft.original)}
          onFinish={submitModel}
        >
          {embeddingRouteRejected && (
            <Alert
              type="error"
              showIcon
              message="嵌入模型的路由包含 CLOUD 渠道"
              description="嵌入必须全部落在 LOCAL 渠道；已拒绝提交，请改用本地渠道，系统不会回退到云端嵌入。"
            />
          )}
          <Form.Item
            name="key"
            label="模型 alias"
            extra="创建后不可改名；更换嵌入空间需要新建 alias。"
            rules={[
              {required: true, message: '请填写模型 alias'},
              {pattern: keyPattern, message: '只允许 1–64 位小写字母、数字或连字符'},
            ]}
          >
            <Input aria-label="模型别名输入" disabled={Boolean(modelDraft.original)} />
          </Form.Item>
          <Form.Item
            name="name"
            label="展示名称"
            rules={[{required: true, message: '请填写名称'}, {max: 128}]}
          >
            <Input aria-label="模型名称输入" />
          </Form.Item>
          <Form.Item name="kind" label="类型" rules={[{required: true}]}>
            <Select
              aria-label="模型类型选择"
              options={llmModelKindValues.map((value) => ({
                value,
                label: value === 'EMBEDDING' ? 'EMBEDDING（仅本地渠道）' : value,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="protocols"
            label="入口协议集合"
            rules={[
              {required: true, message: '至少选择一个协议'},
              {type: 'array', min: 1, max: 4, message: '1–4 个协议'},
            ]}
          >
            <Select
              mode="multiple"
              aria-label="入口协议集合选择"
              options={llmProtocolValues.map((value) => ({value, label: value}))}
            />
          </Form.Item>
          <Form.Item
            name="embeddingSpaceId"
            label="嵌入空间"
            extra={editingEmbedding
              ? '该模型已绑定嵌入空间，不可原地更换；需要新空间请新建 alias。'
              : 'CHAT 模型必须为空；EMBEDDING 必填，且不能只用维度代替。'}
            rules={[
              {max: 64},
              {
                validator: (_, value: string) => {
                  const kind = modelForm.getFieldValue('kind')
                  if (kind === 'EMBEDDING' && !value?.trim() && !editingEmbedding) {
                    return Promise.reject(new Error('EMBEDDING 模型必须提供嵌入空间'))
                  }
                  if (kind === 'CHAT' && value?.trim()) {
                    return Promise.reject(new Error('CHAT 模型的嵌入空间必须为空'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input
              aria-label="嵌入空间输入"
              disabled={Boolean(editingEmbedding)}
            />
          </Form.Item>
          <Form.Item
            name="dimensions"
            label="向量维度"
            extra="必须等于部署实际维度；服务端复核。"
            rules={[
              {
                validator: (_, value: number | null) => {
                  const kind = modelForm.getFieldValue('kind')
                  if (kind === 'CHAT') {
                    return value === null || value === undefined
                      ? Promise.resolve()
                      : Promise.reject(new Error('CHAT 模型的维度必须为空'))
                  }
                  if (!Number.isInteger(value) || (value as number) < 1) {
                    return Promise.reject(new Error('EMBEDDING 模型需要正整数维度'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <InputNumber
              aria-label="向量维度输入"
              min={1}
              precision={0}
              style={{width: '100%'}}
              disabled={editingEmbedding}
            />
          </Form.Item>
          <Form.Item
            name="allowedSubjects"
            label="可调用的服务主体"
            extra="已验证 SERVICE subject 列表，1–100；不是请求方自报身份。"
            rules={[
              {required: true, message: '至少填写一个服务主体'},
              {type: 'array', min: 1, max: 100, message: '1–100 个主体'},
            ]}
          >
            <Select mode="tags" aria-label="服务主体输入" open={false} tokenSeparators={[',']} />
          </Form.Item>
          <Form.List
            name="routes"
            rules={[
              {
                validator: (_, value: LlmModelRoute[]) => (value?.length >= 1 && value.length <= 16
                  ? Promise.resolve()
                  : Promise.reject(new Error('需要 1–16 条渠道路由；无匹配不会扩大到全部渠道'))),
              },
            ]}
          >
            {(fields, {add, remove: drop}) => (
              <Space direction="vertical" style={{width: '100%'}}>
                {fields.map((field) => (
                  <Space key={field.key} align="baseline" wrap>
                    <Form.Item
                      name={[field.name, 'channelKey']}
                      label="渠道"
                      rules={[{required: true, message: '请选择渠道'}]}
                    >
                      <Select
                        aria-label="路由渠道选择"
                        style={{minWidth: 200}}
                        options={channels.data.items.map((channel) => ({
                          value: channel.key,
                          label: `${channel.key} · ${channel.deployment}`,
                        }))}
                      />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'upstreamModel']}
                      label="上游模型名"
                      rules={[{required: true, message: '请填写上游模型名'}, {max: 128}]}
                    >
                      <Input aria-label="上游模型名输入" />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'capabilities']}
                      label="能力"
                      rules={[{required: true, type: 'array', min: 1, message: '至少一个能力'}]}
                    >
                      <Select
                        mode="multiple"
                        aria-label="路由能力选择"
                        style={{minWidth: 220}}
                        options={llmCapabilityValues.map((value) => ({value, label: value}))}
                      />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'priority']}
                      label="优先级"
                      rules={[{required: true}, {type: 'integer', min: 0, max: 1000, message: '0–1000'}]}
                    >
                      <InputNumber aria-label="路由优先级输入" min={0} max={1000} />
                    </Form.Item>
                    <Form.Item
                      name={[field.name, 'weight']}
                      label="权重"
                      rules={[{required: true}, {type: 'integer', min: 1, max: 1000, message: '1–1000'}]}
                    >
                      <InputNumber aria-label="路由权重输入" min={1} max={1000} />
                    </Form.Item>
                    <Button size="small" onClick={() => drop(field.name)}>移除路由</Button>
                  </Space>
                ))}
                <Button
                  size="small"
                  disabled={fields.length >= 16}
                  onClick={() => add({channelKey: '', upstreamModel: '', capabilities: [], priority: 100, weight: 1})}
                >
                  增加路由
                </Button>
              </Space>
            )}
          </Form.List>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch aria-label="模型启用开关" />
          </Form.Item>
          {modelDraft.original && (
            <Descriptions size="small" column={1} items={[{
              key: 'revision',
              label: '当前版本',
              children: modelDraft.original.revision,
            }]} />
          )}
        </Form>
      </Drawer>
    </Space>
  )
}
