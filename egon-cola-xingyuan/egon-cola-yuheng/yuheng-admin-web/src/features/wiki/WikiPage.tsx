import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  List,
  Popconfirm,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd'
import {useState} from 'react'
import {Link, useParams, useSearchParams} from 'react-router-dom'
import {knowledgeApi} from '../../api/knowledge'
import {GatewayApiError} from '../../api/client'
import {wikiApi, type WikiPageView, type WikiSource} from '../../api/wiki'
import {useCapability} from '../../app/capabilities'
import {LoadingBlock, QueryFailure} from '../../components/QueryState'
import {WikiGraphPanel} from './WikiGraphPanel'

const isForbidden = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 403
const isConflict = (error: unknown): boolean =>
  error instanceof GatewayApiError && error.status === 409

interface DraftState {
  title: string
  markdown: string
  tags: string[]
  links: string[]
  sources: WikiSource[]
}

const fromPage = (page: WikiPageView): DraftState => ({
  title: page.title,
  markdown: page.markdown,
  tags: page.tags,
  links: page.links,
  sources: page.sources,
})

// 阅读/编辑 Wiki：DIRECT 策略直接发布，不显示任何“审核通过/拒绝”动作（原业务Spec §12.2）。
export const WikiPage = () => {
  const {kbId = '', pageId = ''} = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const revisionId = searchParams.get('revision') ?? undefined
  const queryClient = useQueryClient()
  const canWrite = useCapability('yuheng:knowledge:write')
  const [draft, setDraft] = useState<DraftState>()

  const kb = useQuery({
    queryKey: ['knowledge', kbId, 'detail'],
    queryFn: ({signal}) => knowledgeApi.knowledgeBase(kbId, signal),
    enabled: Boolean(kbId),
  })
  const page = useQuery({
    queryKey: ['wiki', kbId, pageId, {revisionId}],
    queryFn: ({signal}) => wikiApi.page(kbId, pageId, revisionId, signal),
    enabled: Boolean(kbId && pageId),
  })
  const pages = useQuery({
    queryKey: ['wiki', kbId, 'pages', {includeDraft: canWrite}],
    queryFn: ({signal}) => wikiApi.pages(kbId, {includeDraft: canWrite}, signal),
    enabled: Boolean(kbId),
  })

  const membership = kb.data?.myRole
  const canEdit = canWrite && (membership === 'EDITOR' || membership === 'OWNER')

  const clearSensitiveCache = () => {
    setDraft(undefined)
    queryClient.removeQueries({queryKey: ['wiki', kbId]})
    queryClient.removeQueries({queryKey: ['knowledge', kbId]})
  }
  const refreshPage = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey: ['wiki', kbId, pageId]}),
      queryClient.invalidateQueries({queryKey: ['wiki', kbId, 'pages']}),
    ])
  }
  const saveDraft = useMutation({
    mutationFn: (state: DraftState) => wikiApi.replaceDraft(kbId, pageId, {
      title: state.title,
      markdown: state.markdown,
      tags: state.tags,
      links: state.links,
      sources: state.sources,
      expectedRevision: page.data!.revision,
    }),
    retry: false,
    onSuccess: async () => {
      setDraft(undefined)
      await refreshPage()
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const publish = useMutation({
    mutationFn: () => wikiApi.publish(kbId, pageId, {
      draftRevisionId: page.data!.draftRevisionId!,
      expectedRevision: page.data!.revision,
    }),
    retry: false,
    onSuccess: async () => {
      setDraft(undefined)
      await refreshPage()
    },
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })
  const unpublish = useMutation({
    mutationFn: () => wikiApi.unpublish(kbId, pageId, page.data!.revision),
    retry: false,
    onSuccess: refreshPage,
    onError: (error) => {
      if (isForbidden(error)) clearSensitiveCache()
    },
  })

  if (page.isLoading) return <LoadingBlock />
  if (page.isError) {
    return (
      <QueryFailure
        error={page.error}
        retry={() => {
          if (isForbidden(page.error)) clearSensitiveCache()
          void page.refetch()
        }}
      />
    )
  }

  const current = page.data
  if (!current) return <LoadingBlock />
  const editing = draft !== undefined
  const view = draft ?? fromPage(current)
  const mutationError = saveDraft.error ?? publish.error ?? unpublish.error

  return (
    <Space align="start" size="middle" style={{width: '100%'}} wrap>
      <Card title="主题列表" size="small" style={{width: 240}}>
        {pages.isError && <QueryFailure error={pages.error} />}
        <List
          size="small"
          dataSource={pages.data?.items ?? []}
          locale={{emptyText: '暂无 Wiki 页面'}}
          renderItem={(item) => (
            <List.Item key={item.id}>
              <Space direction="vertical" size={0}>
                <Link to={`/knowledge/${kbId}/wiki/${item.id}`}>{item.title}</Link>
                <Typography.Text type="secondary">{item.slug}</Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      </Card>
      <Card
        title={current.title}
        style={{flex: '1 1 480px'}}
        extra={(
          <Space wrap>
            <Tag>{current.publicationStatus ?? 'NO_CONTENT'}</Tag>
            <Tag>{`审核：${current.reviewStatus ?? '—'}`}</Tag>
            <Tag>{`策略：${current.publicationPolicySnapshot ?? '—'}`}</Tag>
            {current.publicationErrorCode && <Tag>{`失败：${current.publicationErrorCode}`}</Tag>}
            {current.stale && <Badge status="warning" text="来源已变化" />}
          </Space>
        )}
      >
        <Space direction="vertical" size="small" style={{width: '100%'}}>
          {revisionId && (
            <Alert
              type="info"
              showIcon
              title={`正在查看历史版本 ${revisionId}`}
              description="历史版本只读；恢复内容需要保存为新草稿后重新发布。"
            />
          )}
          {!canEdit && (
            <Alert
              type="info"
              showIcon
              title="只读模式"
              description="当前账号在本知识库不是 EDITOR/OWNER，只能阅读；编辑、发布与下线不可用。"
            />
          )}
          {mutationError && (
            <Alert
              type="error"
              showIcon
              title={mutationError instanceof Error ? mutationError.message : '操作失败'}
              description={isConflict(mutationError)
                ? '页面已被他人更新：你的草稿已保留在本地，请刷新比较后再提交。'
                : undefined}
            />
          )}
          {editing ? (
            <>
              <Input
                aria-label="页面标题"
                value={view.title}
                maxLength={128}
                onChange={(event) => setDraft({...view, title: event.target.value})}
              />
              <Input.TextArea
                aria-label="Markdown 正文"
                rows={16}
                value={view.markdown}
                onChange={(event) => setDraft({...view, markdown: event.target.value})}
              />
              <Select
                mode="tags"
                aria-label="标签"
                style={{width: '100%'}}
                value={view.tags}
                maxTagCount={20}
                onChange={(tags: string[]) => setDraft({...view, tags})}
                options={view.tags.map((tag) => ({value: tag, label: tag}))}
              />
              <Space wrap>
                <Button
                  type="primary"
                  loading={saveDraft.isPending}
                  disabled={saveDraft.isPending || !view.markdown.trim()}
                  onClick={() => saveDraft.mutate(view)}
                >
                  保存草稿
                </Button>
                <Button onClick={() => setDraft(undefined)}>放弃修改</Button>
              </Space>
            </>
          ) : (
            <Typography.Paragraph style={{whiteSpace: 'pre-wrap'}}>
              {current.markdown}
            </Typography.Paragraph>
          )}
          <Space wrap>
            <Button
              disabled={!canEdit || Boolean(revisionId) || editing}
              title={!canEdit ? '当前角色只能阅读，不能编辑内容' : undefined}
              onClick={() => setDraft(fromPage(current))}
            >
              编辑草稿
            </Button>
            <Popconfirm
              title="直接发布该草稿？"
              description="DIRECT 策略无需人工审核，发布后立即对有权成员可见。"
              okText="确认发布"
              onConfirm={() => publish.mutate()}
              disabled={!canEdit || !current.draftRevisionId}
            >
              <Button
                type="primary"
                loading={publish.isPending}
                disabled={!canEdit || !current.draftRevisionId || publish.isPending}
                title={current.draftRevisionId ? undefined : '没有待发布草稿'}
              >
                直接发布
              </Button>
            </Popconfirm>
            <Popconfirm
              title="下线当前发布版本？"
              description="下线只改变可见性，不物理删除版本与审计。"
              okText="确认下线"
              onConfirm={() => unpublish.mutate()}
              disabled={!canEdit || !current.publishedRevisionId}
            >
              <Button
                danger
                loading={unpublish.isPending}
                disabled={!canEdit || !current.publishedRevisionId || unpublish.isPending}
              >
                下线内容
              </Button>
            </Popconfirm>
            {isConflict(mutationError) && (
              <Button onClick={() => void refreshPage()}>刷新比较</Button>
            )}
          </Space>
          <Typography.Text type="secondary">
            {`页面版本 ${current.revision} · 发布版本 ${current.publicationVersion ?? '—'} · 正文版本 ${current.contentRevisionId ?? '—'}`}
          </Typography.Text>
        </Space>
      </Card>
      <Space direction="vertical" size="middle" style={{width: 280}}>
        <Card title="来源" size="small">
          <List
            size="small"
            dataSource={current.sources}
            locale={{emptyText: '无来源'}}
            renderItem={(source) => (
              <List.Item key={`${source.documentRevisionId}:${source.chunkId}`}>
                <Space direction="vertical" size={0}>
                  <Typography.Text>{`资料版本 ${source.documentRevisionId}`}</Typography.Text>
                  <Typography.Text type="secondary">{`分块 ${source.chunkId}`}</Typography.Text>
                  <Typography.Text type="secondary">{`hash ${source.sourceHash.slice(0, 12)}…`}</Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        </Card>
        <WikiGraphPanel kbId={kbId} pageId={pageId} />
      </Space>
    </Space>
  )
}
