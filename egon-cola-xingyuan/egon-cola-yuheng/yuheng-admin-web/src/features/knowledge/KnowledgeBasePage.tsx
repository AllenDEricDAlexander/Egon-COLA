import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'antd'
import {useState} from 'react'
import {Link, useParams, useSearchParams} from 'react-router-dom'
import {
  knowledgeApi,
  knowledgeEgressPolicyValues,
  knowledgeMemberRoleValues,
  type KnowledgeDocument,
  type KnowledgeMember,
} from '../../api/knowledge'
import {GatewayApiError} from '../../api/client'
import {wikiApi} from '../../api/wiki'
import {useCapability} from '../../app/capabilities'
import {EmptyBlock, LoadingBlock, QueryFailure} from '../../components/QueryState'
import {KnowledgeAnswerPanel} from './KnowledgeAnswerPanel'
import {KnowledgeJobsPanel} from './KnowledgeJobsPanel'

const isForbidden = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 403
const isConflict = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 409

type SettingsForm = {
  name: string
  description: string
  egressPolicy: string
  chatModel: string
  embeddingModel: string
}

// 员工知识工作区：与旧管理 guard 独立，权限来自服务端返回的 myRole + 知识能力。
export const KnowledgeBasePage = () => {
  const {kbId = ''} = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const canWrite = useCapability('yuheng:knowledge:write')
  const canAdmin = useCapability('yuheng:knowledge:admin')
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadTarget, setUploadTarget] = useState<KnowledgeDocument>()
  const [file, setFile] = useState<File>()
  const [generateOpen, setGenerateOpen] = useState(false)
  const [generateForm] = Form.useForm<{sourceRevisionIds: string[]; pageId?: string}>()

  const kb = useQuery({
    queryKey: ['knowledge', kbId, 'detail'],
    queryFn: ({signal}) => knowledgeApi.knowledgeBase(kbId, signal),
    enabled: Boolean(kbId),
  })
  const documents = useQuery({
    queryKey: ['knowledge', kbId, 'documents'],
    queryFn: ({signal}) => knowledgeApi.documents(kbId, 1, 20, signal),
    enabled: Boolean(kbId),
  })
  const members = useQuery({
    queryKey: ['knowledge', kbId, 'members'],
    queryFn: ({signal}) => knowledgeApi.members(kbId, signal),
    enabled: canAdmin && kb.data?.myRole === 'OWNER',
  })
  const wikiPages = useQuery({
    queryKey: ['wiki', kbId, 'pages', {includeDraft: canWrite}],
    queryFn: ({signal}) => wikiApi.pages(kbId, {includeDraft: canWrite}, signal),
    enabled: Boolean(kbId),
  })

  const membership = kb.data?.myRole
  const isEditor = canWrite && (membership === 'EDITOR' || membership === 'OWNER')
  const isOwner = canAdmin && membership === 'OWNER'

  // 403/失权与登出一样的处理：立即清除已缓存正文与派生结果，不留可读快照。
  const clearSensitiveCache = () => {
    queryClient.removeQueries({queryKey: ['knowledge', kbId]})
    queryClient.removeQueries({queryKey: ['wiki', kbId]})
    queryClient.removeQueries({queryKey: ['job']})
  }
  const refreshKnowledge = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'documents']}),
      queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'jobs']}),
      queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'detail']}),
    ])
  }

  const upload = useMutation({
    mutationFn: (target: {file: File; documentId?: string; expectedRevision?: number}) => {
      const form = new FormData()
      form.set('file', target.file)
      if (target.documentId) {
        form.set('documentId', target.documentId)
        form.set('expectedRevision', String(target.expectedRevision))
      }
      return knowledgeApi.uploadDocument(kbId, form)
    },
    retry: false,
    onSuccess: async (receipt) => {
      setUploadOpen(false)
      setFile(undefined)
      setUploadTarget(undefined)
      await Promise.all([
        refreshKnowledge(),
        queryClient.invalidateQueries({queryKey: ['job', receipt.jobId]}),
      ])
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const remove = useMutation({
    mutationFn: (document: KnowledgeDocument) =>
      knowledgeApi.deleteDocument(kbId, document.id, document.revision),
    retry: false,
    onSuccess: refreshKnowledge,
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const reindex = useMutation({
    mutationFn: (document: KnowledgeDocument) => knowledgeApi.createReindexJob(
      kbId,
      document.id,
      {sourceRevisionId: document.activeRevisionId!, expectedRevision: document.revision},
    ),
    retry: false,
    onSuccess: async (job) => {
      await Promise.all([
        refreshKnowledge(),
        queryClient.invalidateQueries({queryKey: ['job', job.id]}),
      ])
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const saveSettings = useMutation({
    mutationFn: (values: SettingsForm) => knowledgeApi.replaceKnowledgeBase(kbId, {
      ...values,
      egressPolicy: values.egressPolicy,
      expectedRevision: kb.data!.revision,
    }),
    retry: false,
    onSuccess: async () => {
      await queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'detail']})
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const saveMembers = useMutation({
    mutationFn: (values: {members: KnowledgeMember[]}) => knowledgeApi.replaceMembers(kbId, {
      members: values.members,
      expectedRevision: members.data!.revision,
    }),
    retry: false,
    onSuccess: async () => {
      await queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'members']})
    },
  })
  const generate = useMutation({
    mutationFn: (values: {sourceRevisionIds: string[]; pageId?: string}) =>
      wikiApi.createGenerationJob(kbId, {
        sourceRevisionIds: values.sourceRevisionIds,
        pageId: values.pageId || null,
        expectedRevision: 0,
      }),
    retry: false,
    onSuccess: async (job) => {
      setGenerateOpen(false)
      await Promise.all([
        queryClient.invalidateQueries({queryKey: ['wiki', kbId]}),
        queryClient.invalidateQueries({queryKey: ['knowledge', kbId, 'jobs']}),
        queryClient.invalidateQueries({queryKey: ['job', job.id]}),
      ])
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })

  if (kb.isLoading || documents.isLoading) return <LoadingBlock />
  if (kb.isError) {
    return (
      <QueryFailure
        error={kb.error}
        retry={() => {
          if (isForbidden(kb.error)) clearSensitiveCache()
          void kb.refetch()
        }}
      />
    )
  }
  if (!kb.data) return <LoadingBlock />

  const settingsError = saveSettings.error ?? saveMembers.error
  const tab = searchParams.get('tab') ?? 'documents'

  return (
    <Card
      title={kb.data.name}
      extra={(
        <Space wrap>
          <Tag>{`我的角色：${kb.data.myRole}`}</Tag>
          <Tag>{`出域策略：${kb.data.egressPolicy}`}</Tag>
          <Tag>{`嵌入空间：${kb.data.embeddingSpaceId ?? '—'} · ${kb.data.dimensions ?? '—'} 维`}</Tag>
        </Space>
      )}
    >
      <Tabs
        activeKey={tab}
        onChange={(next) => setSearchParams({tab: next})}
        items={[
          {
            key: 'documents',
            label: '资料列表',
            children: (
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
                <Space wrap>
                  <Button
                    type="primary"
                    disabled={!isEditor || upload.isPending}
                    title={isEditor ? undefined : '当前角色不能上传资料'}
                    onClick={() => setUploadOpen(true)}
                  >
                    上传资料
                  </Button>
                  <Button onClick={() => void documents.refetch()}>刷新资料</Button>
                  {remove.isError && (
                    <Alert
                      type="error"
                      showIcon
                      title={remove.error instanceof Error ? remove.error.message : '删除失败'}
                      description={isConflict(remove.error) ? '版本已变化，请刷新后确认。' : undefined}
                    />
                  )}
                </Space>
                {documents.isError ? (
                  <QueryFailure error={documents.error} />
                ) : (
                  <Table<KnowledgeDocument>
                    rowKey="id"
                    dataSource={documents.data?.items ?? []}
                    pagination={false}
                    locale={{emptyText: <EmptyBlock description="暂无资料" />}}
                    columns={[
                      {title: '文件名', dataIndex: 'fileName'},
                      {
                        title: '当前版本',
                        dataIndex: 'activeRevisionId',
                        render: (value: string | null) => value ?? '尚无可见版本',
                      },
                      {title: '最近任务', dataIndex: 'latestJobId', render: (v: string | null) => v ?? '—'},
                      {
                        title: '状态',
                        dataIndex: 'status',
                        render: (value: string) => <Tag>{value}</Tag>,
                      },
                      {
                        title: '操作',
                        render: (_, record) => (
                          <Space wrap>
                            <Button
                              size="small"
                              disabled={!isEditor || !record.activeRevisionId || reindex.isPending}
                              onClick={() => reindex.mutate(record)}
                            >
                              重建索引
                            </Button>
                            <Popconfirm
                              title="让该资料退出可见知识？"
                              description="逻辑删除后历史仍保留，但不再参与检索。"
                              okText="确认删除"
                              onConfirm={() => remove.mutate(record)}
                              disabled={!isEditor}
                            >
                              <Button size="small" danger disabled={!isEditor}>
                                删除资料
                              </Button>
                            </Popconfirm>
                          </Space>
                        ),
                      },
                    ]}
                  />
                )}
              </Space>
            ),
          },
          {
            key: 'wiki',
            label: 'Wiki 主题',
            children: (
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
                <Space wrap>
                  <Button
                    type="primary"
                    disabled={!isEditor || generate.isPending}
                    onClick={() => setGenerateOpen(true)}
                  >
                    生成页面
                  </Button>
                  <Button onClick={() => void wikiPages.refetch()}>刷新列表</Button>
                  {generate.isError && (
                    <Alert
                      type="error"
                      showIcon
                      title={generate.error instanceof Error ? generate.error.message : '生成失败'}
                    />
                  )}
                </Space>
                {wikiPages.isError ? (
                  <QueryFailure error={wikiPages.error} />
                ) : (
                  <Table
                    rowKey="id"
                    dataSource={wikiPages.data?.items ?? []}
                    pagination={false}
                    locale={{emptyText: <EmptyBlock description="暂无 Wiki 页面" />}}
                    columns={[
                      {
                        title: '标题',
                        dataIndex: 'title',
                        render: (_, record) => (
                          <Link to={`/knowledge/${kbId}/wiki/${record.id}`}>{record.title}</Link>
                        ),
                      },
                      {title: '逻辑地址', dataIndex: 'slug'},
                      {title: '发布状态', dataIndex: 'publicationStatus', render: (v: string | null) => v ?? '—'},
                      {title: '审核状态', dataIndex: 'reviewStatus', render: (v: string | null) => v ?? '—'},
                      {title: '策略快照', dataIndex: 'publicationPolicySnapshot', render: (v: string | null) => v ?? '—'},
                      {
                        title: '来源状态',
                        dataIndex: 'stale',
                        render: (value: boolean) => (value ? <Tag>来源已变化</Tag> : <Tag>来源有效</Tag>),
                      },
                    ]}
                  />
                )}
              </Space>
            ),
          },
          {key: 'ask', label: '知识问答', children: <KnowledgeAnswerPanel kbId={kbId} />},
          {key: 'jobs', label: '处理任务', children: <KnowledgeJobsPanel kbId={kbId} />},
          {
            key: 'settings',
            label: '知识库设置',
            children: (
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
                {!isOwner && (
                  <Alert
                    type="info"
                    showIcon
                    title="只有知识库所有者可修改设置与成员"
                    description={`当前角色：${membership ?? '未知'}；缺少 yuheng:knowledge:admin 能力时始终只读。`}
                  />
                )}
                {settingsError && (
                  <Alert
                    type="error"
                    showIcon
                    title={settingsError instanceof Error ? settingsError.message : '保存失败'}
                    description={isConflict(settingsError)
                      ? '版本已被他人推进，请刷新后再提交。'
                      : undefined}
                  />
                )}
                <Form<SettingsForm>
                  layout="vertical"
                  disabled={!isOwner}
                  initialValues={{
                    name: kb.data.name,
                    description: kb.data.description,
                    egressPolicy: kb.data.egressPolicy,
                    chatModel: kb.data.chatModel,
                    embeddingModel: kb.data.embeddingModel,
                  }}
                  onFinish={(values) => saveSettings.mutate(values)}
                  key={`settings-${kb.data.revision}`}
                >
                  <Form.Item name="name" label="名称" rules={[{required: true}, {max: 128}]}>
                    <Input aria-label="知识库名称输入" />
                  </Form.Item>
                  <Form.Item name="description" label="说明">
                    <Input.TextArea rows={2} aria-label="知识库说明输入" maxLength={2000} />
                  </Form.Item>
                  <Form.Item
                    name="egressPolicy"
                    label="出域策略"
                    extra="只影响生成上下文的出域；嵌入模型始终本地。"
                  >
                    <Select
                      aria-label="出域策略选择"
                      options={knowledgeEgressPolicyValues.map((value) => ({value, label: value}))}
                    />
                  </Form.Item>
                  <Form.Item name="chatModel" label="对话模型 alias">
                    <Input aria-label="对话模型别名" />
                  </Form.Item>
                  <Form.Item
                    name="embeddingModel"
                    label="嵌入模型 alias"
                    extra="已有索引时不可原地更换嵌入空间，需重建。"
                  >
                    <Input aria-label="嵌入模型别名" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" loading={saveSettings.isPending}>
                    保存设置
                  </Button>
                </Form>
                {isOwner && (
                  <MembersEditor
                    kbId={kbId}
                    members={members.data?.members ?? []}
                    disabled={saveMembers.isPending}
                    onSave={(values) => saveMembers.mutate(values)}
                  />
                )}
                <Descriptions
                  size="small"
                  column={2}
                  items={[
                    {key: 'id', label: '知识库 ID', children: kb.data.id},
                    {key: 'owner', label: '所有者', children: kb.data.ownerActorId},
                    {key: 'revision', label: '配置版本', children: kb.data.revision},
                    {key: 'space', label: '嵌入空间', children: kb.data.embeddingSpaceId ?? '—'},
                  ]}
                />
              </Space>
            ),
          },
        ]}
      />
      <Drawer
        open={uploadOpen}
        title="上传资料"
        width={520}
        onClose={() => setUploadOpen(false)}
        extra={(
          <Button
            type="primary"
            disabled={!file || upload.isPending}
            loading={upload.isPending}
            onClick={() => file && upload.mutate({
              file,
              documentId: uploadTarget?.id,
              expectedRevision: uploadTarget?.revision,
            })}
          >
            提交上传
          </Button>
        )}
      >
        <Space direction="vertical" size="middle" style={{width: '100%'}}>
          <Alert
            type="info"
            showIcon
            title="支持 MD/TXT/PDF/DOCX/XLSX，单个 1–20 MiB"
            description="上传成功只代表任务已受理，索引结果在处理任务页查看。"
          />
          {upload.isError && (
            <Alert
              type="error"
              showIcon
              title={upload.error instanceof Error ? upload.error.message : '上传失败'}
              description={isConflict(upload.error) ? '该资料版本已变化，请重新选择。' : undefined}
            />
          )}
          <Input
            type="file"
            aria-label="资料文件"
            accept=".md,.txt,.pdf,.docx,.xlsx"
            onChange={(event) => setFile(event.target.files?.[0])}
          />
          <Input
            aria-label="更新资料编号"
            placeholder="留空表示新建资料"
            value={uploadTarget?.id ?? ''}
            onChange={(event) => setUploadTarget((current) => ({
              ...(current ?? {
                id: '',
                kbId,
                fileName: '',
                activeRevisionId: null,
                latestJobId: null,
                revision: 0,
                status: 'PROCESSING',
                createdAt: '',
              }),
              id: event.target.value,
            }))}
          />
        </Space>
      </Drawer>
      <Drawer
        open={generateOpen}
        title="生成 Wiki 页面"
        width={520}
        onClose={() => setGenerateOpen(false)}
        extra={(
          <Button type="primary" loading={generate.isPending} onClick={() => generateForm.submit()}>
            提交生成
          </Button>
        )}
      >
        <Form form={generateForm} layout="vertical" onFinish={(values) => generate.mutate(values)}>
          <Form.Item
            name="sourceRevisionIds"
            label="来源版本"
            extra="只能选择同 KB 当前 active 且可读的资料版本。"
            rules={[{required: true, message: '请选择来源版本'}]}
          >
            <Select
              mode="tags"
              aria-label="来源版本"
              options={(documents.data?.items ?? [])
                .filter((document) => document.activeRevisionId)
                .map((document) => ({
                  value: document.activeRevisionId as string,
                  label: `${document.fileName} · ${document.activeRevisionId}`,
                }))}
            />
          </Form.Item>
          <Form.Item name="pageId" label="目标页面（可选）">
            <Input aria-label="目标页面编号" />
          </Form.Item>
        </Form>
      </Drawer>
    </Card>
  )
}

const MembersEditor = ({
  kbId,
  members,
  disabled,
  onSave,
}: {
  kbId: string
  members: KnowledgeMember[]
  disabled: boolean
  onSave: (values: {members: KnowledgeMember[]}) => void
}) => {
  const [form] = Form.useForm<{members: KnowledgeMember[]}>()
  return (
    <Card title="成员授权" size="small">
      <Form
        form={form}
        layout="vertical"
        initialValues={{members}}
        key={`members-${kbId}-${members.length}`}
        onFinish={onSave}
      >
        <Form.List name="members">
          {(fields, {add, remove: drop}) => (
            <Space direction="vertical" style={{width: '100%'}}>
              {fields.map((field) => (
                <Space key={field.key} align="baseline" wrap>
                  <Form.Item
                    name={[field.name, 'actorId']}
                    label="成员主体"
                    rules={[{required: true, message: '请填写 actorId'}]}
                  >
                    <Input aria-label="成员主体" />
                  </Form.Item>
                  <Form.Item name={[field.name, 'role']} label="角色" rules={[{required: true}]}>
                    <Select
                      aria-label="成员角色"
                      style={{minWidth: 140}}
                      options={knowledgeMemberRoleValues.map((value) => ({value, label: value}))}
                    />
                  </Form.Item>
                  <Button size="small" onClick={() => drop(field.name)}>
                    移除成员
                  </Button>
                </Space>
              ))}
              <Space>
                <Button size="small" onClick={() => add({actorId: '', role: 'READER'})}>
                  增加成员
                </Button>
                <Button type="primary" htmlType="submit" loading={disabled}>
                  保存成员
                </Button>
              </Space>
            </Space>
          )}
        </Form.List>
      </Form>
    </Card>
  )
}
