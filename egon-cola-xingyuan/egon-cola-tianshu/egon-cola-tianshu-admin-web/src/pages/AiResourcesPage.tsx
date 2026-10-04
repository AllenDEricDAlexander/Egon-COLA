import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { DdcApiError } from '../api/client'
import {
  createAiResource,
  deleteAiResource,
  ddcAiQueryKeys,
  pageAiResources,
} from '../api/ai'
import type {
  DdcAiContent,
  DdcAiCreateCommand,
  DdcAiKind,
  DdcAiResourcePageParams,
  DdcAiResourceSummary,
} from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { formatTime } from '../lib/query'

type Props = { kind: DdcAiKind }

const emptyScope: ScopeValue = { bizCode: '', namespaceCode: '', env: '', appCode: '' }
const PAGE_SIZE = 20

const scopeFromSearch = (params: URLSearchParams): ScopeValue => ({
  bizCode: params.get('bizCode') ?? '',
  namespaceCode: params.get('namespaceCode') ?? '',
  env: params.get('env') ?? '',
  appCode: params.get('appCode') ?? '',
})

const scopeIsComplete = (scope: ScopeValue): boolean =>
  Object.values(scope).every((value) => value.trim() !== '')

const pageNumberFromSearch = (params: URLSearchParams): number => {
  const value = Number(params.get('pageNo') ?? 1)
  return Number.isSafeInteger(value) && value > 0 ? value : 1
}

const isForbidden = (error: unknown): boolean =>
  error instanceof DdcApiError && error.status === 403

const errorMessage = (error: unknown): string =>
  error instanceof Error ? error.message : String(error)

const initialDraft = (kind: DdcAiKind): DdcAiContent => kind === 'PROMPT'
  ? { promptText: '', nodes: [] }
  : {
    promptText: null,
    nodes: [
      { path: '', nodeType: 'DIRECTORY', content: null },
      { path: 'SKILL.md', nodeType: 'FILE', content: '' },
    ],
  }

export default function AiResourcesPage({ kind }: Props) {
  const { message, modal } = App.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [draftScope, setDraftScope] = useState(() => scopeFromSearch(searchParams))
  const [resourceKeyPrefix, setResourceKeyPrefix] = useState(searchParams.get('resourceKeyPrefix') ?? '')
  const [createOpen, setCreateOpen] = useState(false)
  const [createFailure, setCreateFailure] = useState<string>()
  const [resourceKey, setResourceKey] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [description, setDescription] = useState('')
  const [promptText, setPromptText] = useState('')
  const [skillFileText, setSkillFileText] = useState('')
  useEffect(() => {
    setDraftScope(scopeFromSearch(searchParams))
    setResourceKeyPrefix(searchParams.get('resourceKeyPrefix') ?? '')
  }, [searchParams])

  const submittedScope = useMemo(() => scopeFromSearch(searchParams), [searchParams])
  const pageNo = pageNumberFromSearch(searchParams)
  const params: DdcAiResourcePageParams = {
    ...submittedScope,
    kind,
    resourceKeyPrefix: searchParams.get('resourceKeyPrefix') || undefined,
    pageNo,
    pageSize: PAGE_SIZE,
  }
  const completeScope = scopeIsComplete(submittedScope)
  const resourcesQuery = useQuery({
    queryKey: completeScope
      ? ddcAiQueryKeys.resources(params)
      : [...ddcAiQueryKeys.all(), 'resources', 'scope-required', kind],
    queryFn: ({ signal }) => pageAiResources(params, { signal }),
    enabled: completeScope,
  })

  const createMutation = useMutation({
    mutationFn: (command: DdcAiCreateCommand) => createAiResource(command),
    onSuccess: async (result) => {
      message.success('草稿资源已创建')
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
      setCreateOpen(false)
      navigate(`/ai/resources/${encodeURIComponent(result.resource.id)}`)
    },
    onError: (error: unknown) => setCreateFailure(
      isForbidden(error) ? '当前身份无权在此范围创建资源' : errorMessage(error),
    ),
  })

  const deleteMutation = useMutation({
    mutationFn: (resource: DdcAiResourceSummary) =>
      deleteAiResource(resource.id, resource.revision),
    onSuccess: async () => {
      message.success('资源已删除；历史版本仍保留')
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
    },
    onError: (error: unknown) => message.error(
      isForbidden(error) ? '当前身份无权删除此资源' : errorMessage(error),
    ),
  })

  const updateUrl = (scope: ScopeValue, prefix: string, nextPage = 1) => {
    const next = new URLSearchParams()
    Object.entries(scope).forEach(([key, value]) => {
      if (value.trim() !== '') next.set(key, value.trim())
    })
    if (prefix.trim() !== '') next.set('resourceKeyPrefix', prefix.trim())
    if (nextPage > 1) next.set('pageNo', String(nextPage))
    setSearchParams(next)
  }

  const openCreate = () => {
    setResourceKey('')
    setDisplayName('')
    setDescription('')
    setPromptText('')
    setSkillFileText('')
    setCreateFailure(undefined)
    createMutation.reset()
    setCreateOpen(true)
  }

  const confirmDelete = (resource: DdcAiResourceSummary) => {
    modal.confirm({
      title: `确认删除 ${resource.displayName}？`,
      content: '运行读取将立即不可用，历史版本仍会保留；该资源键不会复活为旧资源。',
      okText: '确认删除',
      okButtonProps: { danger: true, loading: deleteMutation.isPending },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(resource),
    })
  }

  const columns: TableColumnsType<DdcAiResourceSummary> = [
    {
      title: '资源键',
      dataIndex: 'resourceKey',
      key: 'resourceKey',
      render: (value: string, row) => <Typography.Text code>{value}</Typography.Text>,
    },
    { title: '名称', dataIndex: 'displayName', key: 'displayName' },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (value: string) => value || '—',
    },
    {
      title: '发布状态',
      key: 'currentVersionNo',
      render: (_: unknown, row) => row.currentVersionNo === null
        ? <Tag>未发布</Tag>
        : <Tag color="green">当前版本 {row.currentVersionNo}</Tag>,
    },
    {
      title: '最后修改',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (value: string) => formatTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/ai/resources/${encodeURIComponent(row.id)}`)}>
            编辑
          </Button>
          <Button danger type="link" onClick={() => confirmDelete(row)}>
            删除
          </Button>
        </Space>
      ),
    },
  ]

  const submitCreate = () => {
    setCreateFailure(undefined)
    const draft = initialDraft(kind)
    if (kind === 'PROMPT') {
      draft.promptText = promptText
    } else {
      draft.nodes = draft.nodes.map((node) => node.path === 'SKILL.md'
        ? { ...node, content: skillFileText }
        : node)
    }
    createMutation.mutate({
      scope: submittedScope,
      kind,
      resourceKey,
      displayName,
      description,
      draft,
    })
  }

  const denied = isForbidden(resourcesQuery.error)
  const queryFailure = resourcesQuery.isError && !denied

  return (
    <Space orientation="vertical" size="large" className="tianshu-ai-page">
      <AdminPageHeader
        title={kind === 'PROMPT' ? 'Prompt 管理' : 'Skill 管理'}
        description="管理当前四维范围内的可编辑草稿。保存草稿不会发布运行版本。"
      />

      <Card>
        <Space orientation="vertical" size="middle" className="tianshu-ai-page__filters">
          <ScopeSelects value={draftScope} onChange={setDraftScope} />
          <Space wrap>
            <Input
              aria-label="资源键前缀"
              placeholder="资源键前缀"
              maxLength={128}
              value={resourceKeyPrefix}
              onChange={(event) => setResourceKeyPrefix(event.target.value)}
              onPressEnter={() => updateUrl(draftScope, resourceKeyPrefix)}
            />
            <Button
              type="primary"
              disabled={!scopeIsComplete(draftScope)}
              onClick={() => updateUrl(draftScope, resourceKeyPrefix)}
            >
              查询
            </Button>
            <Button onClick={() => {
              setDraftScope({ ...emptyScope })
              setResourceKeyPrefix('')
              updateUrl(emptyScope, '')
            }}>
              重置
            </Button>
          </Space>
        </Space>
      </Card>

      {!completeScope && (
        <Alert type="info" showIcon title="请选择完整的业务域、命名空间、环境和应用后查询。" />
      )}
      {denied && (
        <Alert type="error" showIcon title="当前身份无权读取此范围" description={errorMessage(resourcesQuery.error)} />
      )}
      {queryFailure && (
        <Alert type="error" showIcon title="读取资源失败；结果未被当作空列表处理" description={errorMessage(resourcesQuery.error)} />
      )}

      {completeScope && !denied && !queryFailure && (
        <Card
          title={kind === 'PROMPT' ? 'Prompt 草稿资源' : 'Skill 草稿资源'}
          extra={(
            <Button type="primary" disabled={resourcesQuery.isPending} onClick={openCreate}>
              {`新建 ${kind === 'PROMPT' ? 'Prompt' : 'Skill'}`}
            </Button>
          )}
        >
          <Table<DdcAiResourceSummary>
            rowKey="id"
            columns={columns}
            dataSource={resourcesQuery.data?.records ?? []}
            loading={resourcesQuery.isPending || resourcesQuery.isFetching}
            locale={{ emptyText: `当前范围暂无 ${kind === 'PROMPT' ? 'Prompt' : 'Skill'} 资源` }}
            pagination={{
              current: pageNo,
              pageSize: PAGE_SIZE,
              total: resourcesQuery.data?.page.total ?? 0,
              showSizeChanger: false,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (nextPage) => updateUrl(submittedScope, params.resourceKeyPrefix ?? '', nextPage),
            }}
          />
        </Card>
      )}

      <Modal
        title={`新建 ${kind === 'PROMPT' ? 'Prompt' : 'Skill'} 草稿`}
        open={createOpen}
        onCancel={() => {
          if (!createMutation.isPending) setCreateOpen(false)
        }}
        onOk={submitCreate}
        okText="创建草稿"
        cancelText="取消"
        confirmLoading={createMutation.isPending}
        destroyOnHidden
      >
        <Form layout="vertical">
          {createFailure && <Alert type="error" showIcon title={createFailure} />}
          <Form.Item label="资源键" required>
            <Input
              aria-label="资源键"
              maxLength={128}
              value={resourceKey}
              onChange={(event) => setResourceKey(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="显示名称" required>
            <Input
              aria-label="显示名称"
              maxLength={128}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="描述">
            <Input.TextArea
              aria-label="描述"
              maxLength={2048}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </Form.Item>
          {kind === 'PROMPT' ? (
            <Form.Item label="Prompt 正文" required>
              <Input.TextArea
                aria-label="Prompt 正文"
                rows={12}
                value={promptText}
                onChange={(event) => setPromptText(event.target.value)}
              />
            </Form.Item>
          ) : (
            <Form.Item label="SKILL.md 初始正文">
              <Input.TextArea
                aria-label="SKILL.md 初始正文"
                rows={8}
                value={skillFileText}
                onChange={(event) => setSkillFileText(event.target.value)}
              />
              <Typography.Text type="secondary">初始文件名可在编辑器中修改或删除。</Typography.Text>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </Space>
  )
}
