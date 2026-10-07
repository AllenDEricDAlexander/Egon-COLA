import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Descriptions, Input, Modal, Space, Spin, Table, Tag, Tree, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import type { DataNode } from 'antd/es/tree'
import { FileTextOutlined, FolderOpenOutlined, FolderOutlined } from '@ant-design/icons'
import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { DdcApiError } from '../api/client'
import {
  ddcAiQueryKeys,
  getAiResource,
  getAiVersion,
  pageAiPublications,
  pageAiVersions,
  publishAiDraft,
  rollbackAiPublication,
} from '../api/ai'
import type {
  DdcAiContent,
  DdcAiNode,
  DdcAiPublicationResult,
  DdcAiPublishCommand,
  DdcAiResourceResult,
  DdcAiResourceSummary,
  DdcAiRollbackCommand,
  DdcAiVersionResult,
  DdcAiVersionSummary,
} from '../api/types'
import { formatTime } from '../lib/query'
import { uuidV7 } from '../lib/uuid'
import AiPromotionDialog from './AiPromotionDialog'

type Props = {
  resource: DdcAiResourceSummary
  draftDirty: boolean
  resourceUnavailable?: boolean
  onResourceChanged: (resource: DdcAiResourceResult) => void
  onPendingOperationChange?: (pending: boolean) => void
}

type PendingPublicationRequest =
  | Readonly<{ operation: 'PUBLISH'; requestId: string; command: Readonly<DdcAiPublishCommand> }>
  | Readonly<{ operation: 'ROLLBACK'; requestId: string; command: Readonly<DdcAiRollbackCommand> }>

const VERSION_PAGE_SIZE = 20
const PUBLICATION_PAGE_SIZE = 20

const failureMessage = (error: unknown): string =>
  error instanceof Error ? error.message : String(error)

const isUnknownCommit = (error: unknown): boolean =>
  error instanceof DdcApiError && (error.status === 0 || error.status === 503)

const isForbidden = (error: unknown): boolean =>
  error instanceof DdcApiError && error.status === 403

const treeData = (nodes: DdcAiNode[]): DataNode[] => {
  const root: DataNode = {
    key: '',
    title: <span><FolderOpenOutlined aria-hidden="true" /> Skill 文件根目录（目录）</span>,
    isLeaf: false,
    children: [],
  }
  const entries = new Map<string, DataNode>()
  for (const node of nodes) {
    if (node.path === '') continue
    entries.set(node.path, {
      key: node.path,
      title: (
        <span>
          {node.nodeType === 'DIRECTORY'
            ? <FolderOutlined aria-hidden="true" />
            : <FileTextOutlined aria-hidden="true" />}
          {' '}{node.path.slice(node.path.lastIndexOf('/') + 1)}（{node.nodeType === 'DIRECTORY' ? '目录' : '文件'}）
        </span>
      ),
      isLeaf: node.nodeType === 'FILE',
      children: node.nodeType === 'DIRECTORY' ? [] : undefined,
    })
  }
  for (const node of nodes.filter((item) => item.path !== '').slice().sort((a, b) => a.path.localeCompare(b.path))) {
    const parentPath = node.path.includes('/') ? node.path.slice(0, node.path.lastIndexOf('/')) : ''
    const parent = parentPath ? entries.get(parentPath) : root
    const current = entries.get(node.path)
    if (parent && current && Array.isArray(parent.children)) parent.children.push(current)
  }
  return [root]
}

const sameContent = (left: DdcAiContent, right: DdcAiContent): boolean => {
  const normalize = (content: DdcAiContent) => JSON.stringify({
    promptText: content.promptText,
    nodes: content.nodes.slice().sort((a, b) => a.path.localeCompare(b.path)),
  })
  return normalize(left) === normalize(right)
}

const versionPageSummary = (version: DdcAiVersionSummary): string =>
  `版本 ${version.versionNo} · ${version.displayName}`

export default function AiVersionsPanel({
  resource,
  draftDirty,
  resourceUnavailable = false,
  onResourceChanged,
  onPendingOperationChange = () => {},
}: Props) {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [versionPageNo, setVersionPageNo] = useState(1)
  const [publicationPageNo, setPublicationPageNo] = useState(1)
  const [changeNote, setChangeNote] = useState('')
  const [requestIdInput, setRequestIdInput] = useState('')
  const [appliedRequestId, setAppliedRequestId] = useState<string>()
  const [actionFailure, setActionFailure] = useState<string>()
  const [pendingRequest, setPendingRequest] = useState<PendingPublicationRequest | null>(null)
  const [confirmedAction, setConfirmedAction] = useState<PendingPublicationRequest | null>(null)
  const [verifyingRequest, setVerifyingRequest] = useState(false)
  const [lastReceipt, setLastReceipt] = useState<DdcAiPublicationResult | null>(null)
  const [promotionSource, setPromotionSource] = useState<DdcAiVersionSummary | null>(null)

  const versionParams = useMemo(() => ({ pageNo: versionPageNo, pageSize: VERSION_PAGE_SIZE }), [versionPageNo])
  const publicationParams = useMemo(() => ({
    requestId: appliedRequestId,
    pageNo: publicationPageNo,
    pageSize: PUBLICATION_PAGE_SIZE,
  }), [appliedRequestId, publicationPageNo])
  const selectedVersionNo = searchParams.get('versionNo')

  const versionsQuery = useQuery({
    queryKey: ddcAiQueryKeys.versions(resource.id, versionParams),
    queryFn: ({ signal }) => pageAiVersions(resource.id, versionParams, { signal }),
  })
  const selectedVersionQuery = useQuery({
    queryKey: ddcAiQueryKeys.version(resource.id, selectedVersionNo ?? ''),
    queryFn: ({ signal }) => getAiVersion(resource.id, selectedVersionNo!, { signal }),
    enabled: selectedVersionNo !== null,
  })
  const publicationsQuery = useQuery({
    queryKey: ddcAiQueryKeys.publications(resource.id, publicationParams),
    queryFn: ({ signal }) => pageAiPublications(resource.id, publicationParams, { signal }),
  })

  const refreshResource = async (): Promise<void> => {
    try {
      const latest = await getAiResource(resource.id)
      queryClient.setQueryData(ddcAiQueryKeys.resource(resource.id), latest)
      onResourceChanged(latest)
    } catch (error) {
      setActionFailure(`操作已提交，但资源状态刷新失败：${failureMessage(error)}`)
    }
  }

  const publicationMutation = useMutation({
    mutationFn: (request: PendingPublicationRequest) => {
      return request.operation === 'PUBLISH'
        ? publishAiDraft(resource.id, request.requestId, request.command)
        : rollbackAiPublication(resource.id, request.requestId, request.command)
    },
    onSuccess: async (result) => {
      setPendingRequest(null)
      onPendingOperationChange(false)
      setLastReceipt(result)
      setActionFailure(undefined)
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
      await refreshResource()
      message.success(result.operation === 'PUBLISH'
        ? `已发布版本 ${result.targetVersionNo}`
        : `已回滚到版本 ${result.targetVersionNo}`)
    },
    onError: async (error: unknown) => {
      if (isUnknownCommit(error)) {
        onPendingOperationChange(true)
        setActionFailure('提交结果未知。请先核验原请求回执；重试时会复用相同请求号和内容。')
        return
      }
      setPendingRequest(null)
      onPendingOperationChange(false)
      setActionFailure(isForbidden(error) ? '当前身份无权执行此发布操作' : failureMessage(error))
      if (error instanceof DdcApiError && error.status === 409) {
        await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
        await refreshResource()
      }
    },
  })

  const changeVersionUrl = (versionNo: string | null) => {
    const next = new URLSearchParams(searchParams)
    if (versionNo === null) next.delete('versionNo')
    else next.set('versionNo', versionNo)
    setSearchParams(next)
  }

  const selectedHistoryVersion = (version: DdcAiVersionSummary) => {
    changeVersionUrl(version.versionNo)
  }

  const submitRequest = async (request: PendingPublicationRequest): Promise<void> => {
    setPendingRequest(request)
    onPendingOperationChange(true)
    setActionFailure(undefined)
    try {
      await publicationMutation.mutateAsync(request)
    } catch {
      // The mutation error handler retains an idempotency key only when the result is unknown.
    }
  }

  const confirmPublish = () => {
    if (draftDirty || resourceUnavailable || pendingRequest) return
    setConfirmedAction({
      operation: 'PUBLISH',
      requestId: uuidV7(),
      command: { expectedRevision: resource.revision, changeNote },
    })
  }

  const confirmRollback = (version: DdcAiVersionSummary) => {
    if (resourceUnavailable || pendingRequest) return
    setConfirmedAction({
      operation: 'ROLLBACK',
      requestId: uuidV7(),
      command: { expectedRevision: resource.revision, versionNo: version.versionNo, changeNote },
    })
  }

  const submitConfirmedAction = () => {
    const request = confirmedAction
    setConfirmedAction(null)
    if (request) void submitRequest(request)
  }

  const verifyPendingRequest = async () => {
    if (!pendingRequest) return
    setVerifyingRequest(true)
    setActionFailure(undefined)
    try {
      const receipts = await pageAiPublications(resource.id, {
        requestId: pendingRequest.requestId,
        pageNo: 1,
        pageSize: PUBLICATION_PAGE_SIZE,
      })
      const receipt = receipts.records.find((record) => record.operation === pendingRequest.operation)
      if (!receipt) {
        setActionFailure('未找到发布回执；可使用原请求号和相同内容重试。')
        return
      }
      setPendingRequest(null)
      onPendingOperationChange(false)
      setLastReceipt(receipt)
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
      await refreshResource()
    } catch (error) {
      setActionFailure(`核验原请求失败：${failureMessage(error)}`)
    } finally {
      setVerifyingRequest(false)
    }
  }

  const versionColumns: TableColumnsType<DdcAiVersionSummary> = [
    {
      title: '版本',
      dataIndex: 'versionNo',
      key: 'versionNo',
      render: (value: string, row) => (
        <Space>
          <Typography.Text code>{value}</Typography.Text>
          {value === resource.currentVersionNo && <Tag color="green">当前发布</Tag>}
          {row.sourceVersionId && <Tag>来源版本</Tag>}
        </Space>
      ),
    },
    { title: '名称', dataIndex: 'displayName', key: 'displayName' },
    { title: '说明', dataIndex: 'changeNote', key: 'changeNote', render: (value: string) => value || '—' },
    { title: 'SHA-256', dataIndex: 'contentSha256', key: 'contentSha256', render: (value: string) => <Typography.Text code>{value.slice(0, 12)}</Typography.Text> },
    { title: '文件数', dataIndex: 'nodeCount', key: 'nodeCount' },
    { title: '创建人 / 时间', key: 'createdAt', render: (_: unknown, row) => `${row.createdBy} · ${formatTime(row.createdAt)}` },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row) => (
        <Space wrap>
          <Button type="link" onClick={() => selectedHistoryVersion(row)}>
            {`读取版本 ${row.versionNo}`}
          </Button>
          <Button
            type="link"
            disabled={resourceUnavailable || row.versionNo === resource.currentVersionNo || pendingRequest !== null}
            onClick={() => confirmRollback(row)}
          >
            {`回滚到版本 ${row.versionNo}`}
          </Button>
          <Button type="link" disabled={resourceUnavailable || pendingRequest !== null} onClick={() => setPromotionSource(row)}>
            复制发布
          </Button>
        </Space>
      ),
    },
  ]

  const publicationColumns: TableColumnsType<DdcAiPublicationResult> = [
    { title: '操作', dataIndex: 'operation', key: 'operation', render: (value: string) => <Tag>{value}</Tag> },
    { title: '目标版本', dataIndex: 'targetVersionNo', key: 'targetVersionNo', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '上一版本ID', dataIndex: 'previousVersionId', key: 'previousVersionId', render: (value: string | null) => value ?? '—' },
    { title: '来源版本ID', dataIndex: 'sourceVersionId', key: 'sourceVersionId', render: (value: string | null) => value ?? '本地发布' },
    { title: '发生时间', dataIndex: 'createdAt', key: 'createdAt', render: (value: string) => formatTime(value) },
  ]

  const historicalContent = selectedVersionQuery.data?.content
  const historicalNodes = historicalContent?.nodes ?? []
  const readOnlyTree = useMemo(() => treeData(historicalNodes), [historicalNodes])
  const [selectedHistoryPath, setSelectedHistoryPath] = useState('')
  const selectedHistoryNode = historicalNodes.find((node) => node.path === selectedHistoryPath)

  return (
    <Space orientation="vertical" size="large" className="tianshu-ai-page">
      {actionFailure && <Alert type="error" showIcon title="版本操作未完成" description={actionFailure} />}
      {resourceUnavailable && (
        <Alert type="warning" showIcon title="资源状态无法刷新；版本写操作已禁用" />
      )}
      {lastReceipt && (
        <Alert
          type="success"
          showIcon
          title={`操作 ${lastReceipt.operation} 已提交，目标版本 ${lastReceipt.targetVersionNo}`}
          description={(
            <Space wrap>
              <Typography.Text>
                receipt {lastReceipt.id} · resource {lastReceipt.resourceId} · revision {lastReceipt.resourceRevision}
                {` · 来源版本ID ${lastReceipt.sourceVersionId ?? '本地发布'}`}
              </Typography.Text>
              {lastReceipt.operation === 'PROMOTE' && (
                <Tag>{lastReceipt.targetCreated ? '已创建独立目标资源' : '已更新目标发布指针'}</Tag>
              )}
              <Button
                type="link"
                onClick={() => navigate(`/ai/resources/${encodeURIComponent(lastReceipt.resourceId)}?versionNo=${encodeURIComponent(lastReceipt.targetVersionNo)}`)}
              >
                查看目标版本
              </Button>
            </Space>
          )}
        />
      )}
      {pendingRequest && (
        <Alert
          type="warning"
          showIcon
          title="提交结果未知，原请求号已保留"
          description={(
            <Space wrap>
              <Typography.Text code>{pendingRequest.requestId}</Typography.Text>
              <Button loading={verifyingRequest} onClick={() => void verifyPendingRequest()}>核验原请求</Button>
              <Button
                type="primary"
                loading={publicationMutation.isPending}
                onClick={() => void submitRequest(pendingRequest)}
              >
                使用原请求号重试
              </Button>
            </Space>
          )}
        />
      )}

      <Card
        title="不可变版本历史"
        extra={(
          <Space wrap>
            <Input
              aria-label="版本说明"
              placeholder="发布或回滚说明"
              maxLength={512}
              value={changeNote}
              disabled={pendingRequest !== null}
              onChange={(event) => setChangeNote(event.target.value)}
            />
            <Button
              type="primary"
              disabled={draftDirty || resourceUnavailable || pendingRequest !== null}
              loading={publicationMutation.isPending}
              onClick={confirmPublish}
            >
              发布当前草稿
            </Button>
          </Space>
        )}
      >
        {draftDirty && <Alert type="info" showIcon title="本地草稿尚未保存；保存后才可发布。" />}
        {versionsQuery.isError && (
          <Alert type="error" showIcon title="版本列表读取失败" description={isForbidden(versionsQuery.error) ? '当前身份无权读取历史版本' : failureMessage(versionsQuery.error)} />
        )}
        <Table<DdcAiVersionSummary>
          rowKey="versionNo"
          columns={versionColumns}
          dataSource={versionsQuery.data?.records ?? []}
          loading={versionsQuery.isPending || versionsQuery.isFetching}
          locale={{ emptyText: versionsQuery.isError ? '版本列表读取失败' : '暂无已发布版本' }}
          pagination={{
            current: versionPageNo,
            pageSize: VERSION_PAGE_SIZE,
            total: versionsQuery.data?.page.total ?? 0,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 个版本`,
            onChange: setVersionPageNo,
          }}
        />
      </Card>

      {selectedVersionNo !== null && (
        <Card title={`历史快照 v${selectedVersionNo}（只读）`} extra={(
          <Button onClick={() => changeVersionUrl(null)}>关闭快照</Button>
        )}>
          {selectedVersionQuery.isPending && <Spin description="读取固定历史版本" />}
          {selectedVersionQuery.isError && (
            <Alert type="error" showIcon title="固定历史版本不可用" description={failureMessage(selectedVersionQuery.error)} />
          )}
          {selectedVersionQuery.data && (
            <>
              <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
                <Descriptions.Item label="资源ID">{selectedVersionQuery.data.version.resourceId}</Descriptions.Item>
                <Descriptions.Item label="固定版本号">{selectedVersionQuery.data.version.versionNo}</Descriptions.Item>
                <Descriptions.Item label="SHA-256">{selectedVersionQuery.data.version.contentSha256}</Descriptions.Item>
                <Descriptions.Item label="说明">{selectedVersionQuery.data.version.changeNote || '—'}</Descriptions.Item>
                <Descriptions.Item label="来源版本ID">{selectedVersionQuery.data.version.sourceVersionId ?? '—'}</Descriptions.Item>
                <Descriptions.Item label="创建人 / 时间">
                  {`${selectedVersionQuery.data.version.createdBy} · ${formatTime(selectedVersionQuery.data.version.createdAt)}`}
                </Descriptions.Item>
              </Descriptions>
              {resource.kind === 'PROMPT' ? (
                <Input.TextArea
                  aria-label={`历史 Prompt 正文 v${selectedVersionNo}`}
                  value={historicalContent?.promptText ?? ''}
                  rows={16}
                  disabled
                />
              ) : (
                <div className="tianshu-ai-skill-editor">
                  <Card title="历史 Skill 文件树（只读）" className="tianshu-ai-skill-editor__tree">
                    <Tree
                      aria-label={`历史 Skill 文件树 v${selectedVersionNo}`}
                      treeData={readOnlyTree}
                      selectedKeys={selectedHistoryPath ? [selectedHistoryPath] : []}
                      defaultExpandAll
                      onSelect={(keys) => setSelectedHistoryPath(String(keys[0] ?? ''))}
                    />
                  </Card>
                  <Card title={selectedHistoryNode?.path || '选择一个历史文本文件'} className="tianshu-ai-skill-editor__content">
                    {selectedHistoryNode?.nodeType === 'FILE' ? (
                      <Input.TextArea
                        aria-label={`历史文件正文: ${selectedHistoryNode.path}`}
                        value={selectedHistoryNode.content ?? ''}
                        rows={20}
                        disabled
                      />
                    ) : <Typography.Text type="secondary">历史快照只读，不渲染或执行文件内容。</Typography.Text>}
                  </Card>
                </div>
              )}
            </>
          )}
        </Card>
      )}

      <Card title="发布、回滚与推广审计">
        <Space wrap>
          <Input
            aria-label="审计请求号"
            placeholder="可选：按请求号核验回执"
            maxLength={64}
            value={requestIdInput}
            onChange={(event) => setRequestIdInput(event.target.value)}
          />
          <Button onClick={() => {
            setAppliedRequestId(requestIdInput.trim() || undefined)
            setPublicationPageNo(1)
          }}>
            查询审计
          </Button>
        </Space>
        {publicationsQuery.isError && (
          <Alert type="error" showIcon title="发布审计读取失败" description={failureMessage(publicationsQuery.error)} />
        )}
        <Table<DdcAiPublicationResult>
          rowKey="id"
          columns={publicationColumns}
          dataSource={publicationsQuery.data?.records ?? []}
          loading={publicationsQuery.isPending || publicationsQuery.isFetching}
          locale={{ emptyText: publicationsQuery.isError ? '发布审计读取失败' : '暂无发布记录' }}
          pagination={{
            current: publicationPageNo,
            pageSize: PUBLICATION_PAGE_SIZE,
            total: publicationsQuery.data?.page.total ?? 0,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条发布记录`,
            onChange: setPublicationPageNo,
          }}
        />
      </Card>

      {promotionSource && (
        <AiPromotionDialog
          open
          sourceResource={resource}
          sourceVersion={promotionSource}
          onCancel={() => setPromotionSource(null)}
          onPendingChange={onPendingOperationChange}
          onPublished={(result) => {
            setPromotionSource(null)
            onPendingOperationChange(false)
            setLastReceipt(result)
            setActionFailure(undefined)
            void queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
          }}
        />
      )}

      <Modal
        title={confirmedAction?.operation === 'PUBLISH' ? '确认发布当前草稿？'
          : confirmedAction ? `确认回滚到版本 ${confirmedAction.command.versionNo}？` : ''}
        open={confirmedAction !== null}
        onCancel={() => setConfirmedAction(null)}
        onOk={submitConfirmedAction}
        okText={confirmedAction?.operation === 'PUBLISH' ? '确认发布' : '确认回滚'}
        cancelText="取消"
      >
        {confirmedAction?.operation === 'PUBLISH'
          ? `将以修订 ${confirmedAction.command.expectedRevision} 发布当前已保存草稿。未保存内容不会发布。`
          : confirmedAction?.operation === 'ROLLBACK'
            ? `本次操作只切换当前发布指针，不修改未保存草稿或历史版本。预期修订：${confirmedAction.command.expectedRevision}。`
            : null}
      </Modal>
    </Space>
  )
}
