import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileTextOutlined, FolderOpenOutlined, FolderOutlined } from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Spin,
  Tabs,
  Tree,
  Typography,
} from 'antd'
import type { DataNode } from 'antd/es/tree'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import type { BlockerFunction } from 'react-router-dom'
import { DdcApiError } from '../api/client'
import {
  deleteAiResource,
  ddcAiQueryKeys,
  getAiResource,
  replaceAiDraft,
} from '../api/ai'
import type {
  DdcAiContent,
  DdcAiNode,
  DdcAiResourceResult,
  DdcAiResourceSummary,
} from '../api/types'
import { useAuth } from '../auth/AuthContext'
import AdminPageHeader from '../components/page/AdminPageHeader'
import { formatTime } from '../lib/query'
import AiFeedbackPanel from './AiFeedbackPanel'
import AiOptimizationPanel from './AiOptimizationPanel'
import AiVersionsPanel from './AiVersionsPanel'

type DraftBuffer = {
  displayName: string
  description: string
  draft: DdcAiContent
}

type NodeOperation = 'new-file' | 'new-directory' | 'rename' | 'move'

type NodeDialogState = {
  operation: NodeOperation
  sourcePath?: string
}

const toBuffer = (result: DdcAiResourceResult): DraftBuffer => ({
  displayName: result.resource.displayName,
  description: result.resource.description,
  draft: {
    promptText: result.draft.promptText,
    nodes: result.draft.nodes.map((node) => ({ ...node })),
  },
})

const sameBuffer = (left: DraftBuffer | null, right: DraftBuffer | null): boolean =>
  left !== null && right !== null && JSON.stringify(left) === JSON.stringify(right)

const parentPath = (path: string): string => {
  const separator = path.lastIndexOf('/')
  return separator < 0 ? '' : path.slice(0, separator)
}

const basename = (path: string): string => path.slice(path.lastIndexOf('/') + 1)

const isDirectory = (nodes: DdcAiNode[], path: string): boolean =>
  nodes.some((node) => node.path === path && node.nodeType === 'DIRECTORY')

const isValidPath = (path: string): boolean => {
  if (path === '' || path.startsWith('/') || path.endsWith('/') || path.includes('\\')) return false
  const segments = path.split('/')
  return segments.length <= 16
    && segments.every((segment) => segment !== '' && segment !== '.' && segment !== '..'
      && !/[\u0000-\u001f\u007f]/.test(segment))
    && new TextEncoder().encode(path).length <= 1024
}

const replaceSubtreePrefix = (nodes: DdcAiNode[], from: string, to: string): DdcAiNode[] =>
  nodes.map((node) => {
    if (node.path === from) return { ...node, path: to }
    if (node.path.startsWith(`${from}/`)) {
      return { ...node, path: `${to}${node.path.slice(from.length)}` }
    }
    return node
  })

const subTreeSize = (nodes: DdcAiNode[], path: string): number =>
  nodes.filter((node) => node.path === path || node.path.startsWith(`${path}/`)).length

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
          {' '}{basename(node.path)}（{node.nodeType === 'DIRECTORY' ? '目录' : '文件'}）
        </span>
      ),
      isLeaf: node.nodeType === 'FILE',
      children: node.nodeType === 'DIRECTORY' ? [] : undefined,
    })
  }
  const orderedNodes = nodes
    .filter((node) => node.path !== '')
    .slice()
    .sort((left, right) => left.path.localeCompare(right.path))
  for (const node of orderedNodes) {
    const dataNode = entries.get(node.path)
    if (!dataNode) continue
    const parent = node.path.includes('/') ? entries.get(parentPath(node.path)) : root
    if (parent && Array.isArray(parent.children)) parent.children.push(dataNode)
  }
  return [root]
}

const toListUrl = (resource: DdcAiResourceSummary): string => {
  const query = new URLSearchParams({
    bizCode: resource.scope.bizCode,
    namespaceCode: resource.scope.namespaceCode,
    env: resource.scope.env,
    appCode: resource.scope.appCode,
  })
  return `/ai/${resource.kind === 'PROMPT' ? 'prompts' : 'skills'}?${query}`
}

const failureText = (error: unknown): string => {
  if (error instanceof DdcApiError && error.status === 403) return '当前身份无权执行此操作'
  return error instanceof Error ? error.message : String(error)
}

export default function AiResourceEditorPage() {
  const { message, modal } = App.useApp()
  const { error: authError, registerUnsavedChangesGuard } = useAuth()
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [activeTab, setActiveTab] = useState(() => searchParams.has('versionNo') ? 'versions' : 'draft')
  const queryKey = ddcAiQueryKeys.resource(id)
  const resourceQuery = useQuery({
    queryKey,
    queryFn: ({ signal }) => getAiResource(id, { signal }),
    enabled: id !== '',
  })
  const [baseline, setBaseline] = useState<DdcAiResourceResult | null>(null)
  const [buffer, setBuffer] = useState<DraftBuffer | null>(null)
  const [selectedPath, setSelectedPath] = useState('')
  const [saveFailure, setSaveFailure] = useState<string>()
  const [conflict, setConflict] = useState(false)
  const [nodeDialog, setNodeDialog] = useState<NodeDialogState | null>(null)
  const [nodePathValue, setNodePathValue] = useState('')
  const [targetDirectoryPath, setTargetDirectoryPath] = useState('')
  const [logoutConfirmationOpen, setLogoutConfirmationOpen] = useState(false)
  const logoutConfirmationResolver = useRef<((allowed: boolean) => void) | null>(null)
  const [pendingVersionOperation, setPendingVersionOperation] = useState(false)
  const dirtyRef = useRef(false)

  const isDirty = baseline !== null && buffer !== null
    && !sameBuffer(buffer, toBuffer(baseline))
  dirtyRef.current = isDirty

  useEffect(() => {
    if (searchParams.has('versionNo')) setActiveTab('versions')
  }, [searchParams])

  useEffect(() => {
    const server = resourceQuery.data
    if (!server || dirtyRef.current) return
    setBaseline(server)
    setBuffer(toBuffer(server))
    const firstFile = server.draft.nodes.find((node) => node.nodeType === 'FILE')
    setSelectedPath(firstFile?.path ?? '')
  }, [resourceQuery.data])

  useEffect(() => {
    if (!isDirty && !pendingVersionOperation) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty, pendingVersionOperation])

  const leaveGuard = useCallback((): Promise<boolean> => new Promise((resolve) => {
    logoutConfirmationResolver.current = resolve
    setLogoutConfirmationOpen(true)
  }), [])

  const answerLogoutConfirmation = (allowed: boolean) => {
    setLogoutConfirmationOpen(false)
    logoutConfirmationResolver.current?.(allowed)
    logoutConfirmationResolver.current = null
  }

  useEffect(() => {
    if (!isDirty && !pendingVersionOperation) return
    return registerUnsavedChangesGuard(leaveGuard)
  }, [isDirty, leaveGuard, pendingVersionOperation, registerUnsavedChangesGuard])

  const shouldBlock = useCallback<BlockerFunction>(({ currentLocation, nextLocation }) =>
    (isDirty || pendingVersionOperation) && (
    currentLocation.pathname !== nextLocation.pathname
  ), [isDirty, pendingVersionOperation])
  const blocker = useBlocker(shouldBlock)

  const updateDraft = (update: (current: DraftBuffer) => DraftBuffer) => {
    setBuffer((current) => current ? update(current) : current)
    setSaveFailure(undefined)
  }

  const saveMutation = useMutation({
    mutationFn: ({ current, expectedRevision }: {
      current: DraftBuffer
      expectedRevision: string
    }) => replaceAiDraft(id, {
      expectedRevision,
      displayName: current.displayName,
      description: current.description,
      draft: current.draft,
    }),
    onSuccess: async (result) => {
      setBaseline(result)
      setBuffer(toBuffer(result))
      setConflict(false)
      setSaveFailure(undefined)
      queryClient.setQueryData(queryKey, result)
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
      message.success('草稿已保存；当前发布版本未改变')
    },
    onError: async (error: unknown) => {
      if (error instanceof DdcApiError && error.status === 409) {
        setConflict(true)
        setSaveFailure(undefined)
        await resourceQuery.refetch()
      } else {
        setSaveFailure(failureText(error))
      }
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (resource: DdcAiResourceSummary) => deleteAiResource(resource.id, resource.revision),
    onSuccess: async () => {
      const current = baseline?.resource
      if (!current) return
      await queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.all() })
      message.success('资源已删除；历史版本仍会保留')
      navigate(toListUrl(current))
    },
    onError: (error: unknown) => setSaveFailure(failureText(error)),
  })

  const savedBuffer = baseline ? toBuffer(baseline) : null
  const selectedNode = buffer?.draft.nodes.find((node) => node.path === selectedPath)
  const directories = buffer?.draft.nodes.filter((node) => node.nodeType === 'DIRECTORY') ?? []
  const visibleTree = useMemo(() => treeData(buffer?.draft.nodes ?? []), [buffer?.draft.nodes])
  const writable = baseline !== null && buffer !== null && !resourceQuery.isError

  const selectedParent = selectedNode?.nodeType === 'DIRECTORY'
    ? selectedNode.path
    : parentPath(selectedPath)
  const openNewNode = (operation: 'new-file' | 'new-directory') => {
    const label = operation === 'new-file' ? '新建文件.txt' : '新建目录'
    let path = selectedParent ? `${selectedParent}/${label}` : label
    let suffix = 1
    while (buffer?.draft.nodes.some((node) => node.path === path)) {
      path = selectedParent ? `${selectedParent}/${label}-${suffix}` : `${label}-${suffix}`
      suffix += 1
    }
    setNodePathValue(path)
    setTargetDirectoryPath('')
    setNodeDialog({ operation })
  }

  const openRename = () => {
    if (!selectedNode || selectedNode.path === '') return
    setNodePathValue(selectedNode.path)
    setNodeDialog({ operation: 'rename', sourcePath: selectedNode.path })
  }

  const openMove = () => {
    if (!selectedNode || selectedNode.path === '') return
    setTargetDirectoryPath(parentPath(selectedNode.path))
    setNodeDialog({ operation: 'move', sourcePath: selectedNode.path })
  }

  const confirmDeleteNode = () => {
    if (!selectedNode || selectedNode.path === '') return
    const path = selectedNode.path
    const affectedCount = subTreeSize(buffer?.draft.nodes ?? [], path)
    modal.confirm({
      title: `删除 ${path}？`,
      content: `将删除 ${affectedCount} 个节点及其草稿文本；此操作需保存后生效。`,
      okText: '确认删除节点',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => {
        updateDraft((current) => ({
          ...current,
          draft: {
            ...current.draft,
            nodes: current.draft.nodes.filter((node) =>
              node.path !== path && !node.path.startsWith(`${path}/`)),
          },
        }))
        setSelectedPath('')
      },
    })
  }

  const applyNodeOperation = () => {
    if (!nodeDialog || !buffer) return
    let nextNodes = buffer.draft.nodes
    const { operation, sourcePath } = nodeDialog

    if (operation === 'new-file' || operation === 'new-directory') {
      const path = nodePathValue.trim()
      const parent = parentPath(path)
      if (!isValidPath(path) || !isDirectory(nextNodes, parent)
          || nextNodes.some((node) => node.path === path)) {
        message.error('路径无效、父目录不存在或目标路径已存在')
        return
      }
      nextNodes = [...nextNodes, {
        path,
        nodeType: operation === 'new-directory' ? 'DIRECTORY' : 'FILE',
        content: operation === 'new-directory' ? null : '',
      }]
      setSelectedPath(path)
    } else if (operation === 'rename' && sourcePath) {
      const path = nodePathValue.trim()
      const parent = parentPath(path)
      const movingIntoOwnSubtree = buffer.draft.nodes.some((node) =>
        node.path === sourcePath && node.nodeType === 'DIRECTORY'
        && (path === sourcePath || path.startsWith(`${sourcePath}/`)))
      const transformed = replaceSubtreePrefix(nextNodes, sourcePath, path)
      const otherPaths = new Set(transformed.filter((node) =>
        node.path !== path && !node.path.startsWith(`${path}/`)).map((node) => node.path))
      const transformedPaths = transformed.map((node) => node.path)
      const hasDuplicates = new Set(transformedPaths).size !== transformedPaths.length
      if (!isValidPath(path) || !isDirectory(nextNodes, parent) || movingIntoOwnSubtree
          || hasDuplicates || otherPaths.has(path)) {
        message.error('新路径无效、父目录不存在或会与现有节点冲突')
        return
      }
      nextNodes = transformed
      setSelectedPath(path)
    } else if (operation === 'move' && sourcePath) {
      const target = targetDirectoryPath.trim()
      const targetNode = nextNodes.find((node) => node.path === target)
      const sourceNode = nextNodes.find((node) => node.path === sourcePath)
      const path = target ? `${target}/${basename(sourcePath)}` : basename(sourcePath)
      const movingIntoOwnSubtree = sourceNode?.nodeType === 'DIRECTORY'
        && (target === sourcePath || target.startsWith(`${sourcePath}/`))
      const transformed = replaceSubtreePrefix(nextNodes, sourcePath, path)
      const hasDuplicates = new Set(transformed.map((node) => node.path)).size !== transformed.length
      if (!isValidPath(path) || !targetNode || targetNode.nodeType !== 'DIRECTORY'
          || movingIntoOwnSubtree || hasDuplicates) {
        message.error('目标目录无效、移动会形成环或目标路径已存在')
        return
      }
      nextNodes = transformed
      setSelectedPath(path)
    }

    updateDraft((current) => ({ ...current, draft: { ...current.draft, nodes: nextNodes } }))
    setNodeDialog(null)
  }

  const saveDraft = () => {
    if (!buffer || !baseline || resourceQuery.isError || saveMutation.isPending || !isDirty) return
    setSaveFailure(undefined)
    saveMutation.mutate({ current: buffer, expectedRevision: baseline.resource.revision })
  }

  const confirmDeleteResource = () => {
    if (!baseline) return
    modal.confirm({
      title: `确认删除 ${baseline.resource.displayName}？`,
      content: isDirty
        ? '运行读取将立即不可用，历史版本仍会保留；当前未保存的本地修改也会丢弃。'
        : '运行读取将立即不可用，历史版本仍会保留。删除后将返回资源列表。',
      okText: '确认删除',
      okButtonProps: { danger: true, loading: deleteMutation.isPending },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(baseline.resource),
    })
  }

  const updatePublishedResource = (updated: DdcAiResourceResult) => {
    setBaseline(updated)
    queryClient.setQueryData(queryKey, updated)
    if (!dirtyRef.current) setBuffer(toBuffer(updated))
  }

  if (resourceQuery.isPending && !resourceQuery.data) {
    return <Spin fullscreen description="读取 AI 草稿" />
  }
  if (resourceQuery.isError && (!resourceQuery.data || !baseline || !buffer)) {
    return <Alert type="error" showIcon title="无法读取此资源" description={failureText(resourceQuery.error)} />
  }
  if (!resourceQuery.data || !baseline || !buffer) {
    return <Alert type="error" showIcon title="资源内容不可用" />
  }

  const resource = resourceQuery.data.resource
  const latestServer = resourceQuery.data
  const canAcknowledgeConflict = conflict && latestServer.resource.revision !== baseline.resource.revision
  const handleTabChange = (key: string) => {
    setActiveTab(key)
    const next = new URLSearchParams(searchParams)
    if (key === 'draft' || key === 'optimization') {
      next.delete('versionNo')
    } else if (!next.has('versionNo') && resource.currentVersionNo !== null) {
      next.set('versionNo', resource.currentVersionNo)
    }
    setSearchParams(next)
  }

  return (
    <Space orientation="vertical" size="large" className="tianshu-ai-page">
      <AdminPageHeader
        title={resource.kind === 'PROMPT' ? 'Prompt 草稿编辑' : 'Skill 草稿编辑'}
        description="编辑区仅保存纯文本草稿；发布版本只读，草稿保存不会切换运行版本。"
        extra={(
          <Space wrap>
            <Button onClick={() => navigate(toListUrl(resource))}>返回资源列表</Button>
            <Button danger onClick={confirmDeleteResource} disabled={!writable || resourceQuery.isError || pendingVersionOperation || saveMutation.isPending || deleteMutation.isPending}>
              删除资源
            </Button>
          </Space>
        )}
      />

      <Card>
        <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
          <Descriptions.Item label="资源键"><Typography.Text code>{resource.resourceKey}</Typography.Text></Descriptions.Item>
          <Descriptions.Item label="完整范围">
            {`${resource.scope.bizCode} / ${resource.scope.namespaceCode} / ${resource.scope.env} / ${resource.scope.appCode}`}
          </Descriptions.Item>
          <Descriptions.Item label="当前发布版本">
            {resource.currentVersionNo ?? '尚未发布'}
          </Descriptions.Item>
          <Descriptions.Item label="草稿修订">{baseline.resource.revision}</Descriptions.Item>
          <Descriptions.Item label="最后修改">{formatTime(resource.updatedAt)}</Descriptions.Item>
          <Descriptions.Item label="草稿状态">
            {isDirty ? <Typography.Text type="warning">未保存</Typography.Text> : '已保存'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {saveFailure && <Alert type="error" showIcon title="草稿操作失败" description={saveFailure} />}
      {authError && <Alert type="error" showIcon title="身份会话操作失败" description={authError} />}
      {resourceQuery.isError && resourceQuery.data && (
        <Alert
          type="warning"
          showIcon
          title="当前会话无法刷新资源状态；本地编辑仍保留"
          description={failureText(resourceQuery.error)}
        />
      )}
      {conflict && (
        <Alert
          type="warning"
          showIcon
          title={canAcknowledgeConflict
            ? `服务器修订 ${latestServer.resource.revision} 已加载；本地草稿仍保留`
            : '保存冲突；正在读取服务器最新草稿'}
          description={(
            <div className="tianshu-ai-conflict">
              <Typography.Paragraph>请手工比较两份内容。确认后才会使用新修订号保存本地编辑。</Typography.Paragraph>
              <pre>{latestServer.draft.promptText ?? latestServer.draft.nodes.map((node) => `${node.nodeType} ${node.path}\n${node.content ?? ''}`).join('\n')}</pre>
              <Button
                type="primary"
                disabled={!canAcknowledgeConflict}
                onClick={() => {
                  setBaseline(latestServer)
                  setConflict(false)
                }}
              >
                我已比较，继续保存本地修改
              </Button>
            </div>
          )}
        />
      )}

      {(isDirty || pendingVersionOperation) && activeTab === 'versions' && (
        <Alert type="info" showIcon title="本地草稿仍保留；切换历史版本不会覆盖未保存编辑。" />
      )}
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        items={[
          {
            key: 'draft',
            label: '草稿编辑',
            children: (
              <Space orientation="vertical" size="large" className="tianshu-ai-page">
                {resource.kind === 'PROMPT' ? (
                  <Card title="Prompt 正文（纯文本）">
                    <Input.TextArea
                      aria-label="Prompt 正文"
                      rows={20}
                      value={buffer.draft.promptText ?? ''}
                      onChange={(event) => updateDraft((current) => ({
                        ...current,
                        draft: { ...current.draft, promptText: event.target.value },
                      }))}
                    />
                  </Card>
                ) : (
                  <div className="tianshu-ai-skill-editor">
                    <Card title="Skill 文件树" className="tianshu-ai-skill-editor__tree">
                      <Space wrap className="tianshu-ai-skill-editor__actions">
                        <Button onClick={() => openNewNode('new-file')}>新增文件</Button>
                        <Button onClick={() => openNewNode('new-directory')}>新增目录</Button>
                        <Button onClick={openMove} disabled={!selectedNode || selectedNode.path === ''}>移动</Button>
                        <Button onClick={openRename} disabled={!selectedNode || selectedNode.path === ''}>重命名</Button>
                        <Button danger onClick={confirmDeleteNode} disabled={!selectedNode || selectedNode.path === ''}>删除节点</Button>
                      </Space>
                      <Tree
                        aria-label="Skill 文件树"
                        treeData={visibleTree}
                        selectedKeys={selectedPath ? [selectedPath] : []}
                        defaultExpandAll
                        onSelect={(keys) => {
                          const nextPath = String(keys[0] ?? '')
                          if (nextPath === selectedPath) return
                          if (!isDirty) {
                            setSelectedPath(nextPath)
                            return
                          }
                          modal.confirm({
                            title: '当前草稿有未保存修改',
                            content: '继续切换文件会保留当前本地树和文本，确认后再选择其他节点。',
                            okText: '保留并切换',
                            cancelText: '留在当前文件',
                            onOk: () => setSelectedPath(nextPath),
                          })
                        }}
                      />
                    </Card>
                    <Card title={selectedNode?.nodeType === 'FILE' ? selectedPath : '选择一个文本文件'} className="tianshu-ai-skill-editor__content">
                      {selectedNode?.nodeType === 'FILE' ? (
                        <Input.TextArea
                          aria-label={`文件正文: ${selectedPath}`}
                          rows={22}
                          value={selectedNode.content ?? ''}
                          onChange={(event) => updateDraft((current) => ({
                            ...current,
                            draft: {
                              ...current.draft,
                              nodes: current.draft.nodes.map((node) => node.path === selectedPath
                                ? { ...node, content: event.target.value }
                                : node),
                            },
                          }))}
                        />
                      ) : (
                        <Typography.Text type="secondary">目录没有正文；所有文件均作为原始文本编辑。</Typography.Text>
                      )}
                    </Card>
                  </div>
                )}
                <Space>
                  <Button
                    type="primary"
                    onClick={saveDraft}
                    disabled={!isDirty || resourceQuery.isError || pendingVersionOperation || saveMutation.isPending || conflict}
                    loading={saveMutation.isPending}
                  >
                    保存草稿
                  </Button>
                  <Button
                    disabled={!isDirty || saveMutation.isPending}
                    onClick={() => {
                      const saved = savedBuffer
                      if (!saved) return
                      modal.confirm({
                        title: '放弃本地草稿修改？',
                        content: '本地未保存的内容将被服务器草稿替换。',
                        okText: '放弃修改',
                        cancelText: '继续编辑',
                        onOk: () => {
                          setBuffer(saved)
                          setBaseline(resourceQuery.data)
                          setConflict(false)
                          setSaveFailure(undefined)
                        },
                      })
                    }}
                  >
                    放弃修改
                  </Button>
                </Space>
              </Space>
            ),
          },
          {
            key: 'versions',
            label: '版本与发布',
            children: (
              <AiVersionsPanel
                resource={resource}
                draftDirty={isDirty}
                resourceUnavailable={resourceQuery.isError}
                onResourceChanged={updatePublishedResource}
                onPendingOperationChange={setPendingVersionOperation}
              />
            ),
          },
          {
            key: 'optimization',
            label: '反馈与优化',
            children: (
              <Space orientation="vertical" size="large" className="tianshu-ai-page">
                <AiOptimizationPanel resourceId={resource.id} />
                <AiFeedbackPanel resourceId={resource.id} />
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={nodeDialog?.operation === 'new-file' ? '新增文本文件'
          : nodeDialog?.operation === 'new-directory' ? '新增目录'
            : nodeDialog?.operation === 'rename' ? '重命名节点' : '移动节点'}
        open={nodeDialog !== null}
        onCancel={() => setNodeDialog(null)}
        onOk={applyNodeOperation}
        okText={nodeDialog?.operation === 'new-file' || nodeDialog?.operation === 'new-directory'
          ? '确认新增' : nodeDialog?.operation === 'rename' ? '确认重命名' : '确认移动'}
        cancelText="取消"
        destroyOnHidden
      >
        {nodeDialog?.operation === 'move' ? (
          <Form layout="vertical">
            <Form.Item label="目标目录路径" help="留空表示根目录；目标必须是现有目录。">
              <Input
                aria-label="目标目录路径"
                value={targetDirectoryPath}
                onChange={(event) => setTargetDirectoryPath(event.target.value)}
              />
            </Form.Item>
            <Typography.Text type="secondary">
              可选目录：{directories.map((directory) => directory.path || '（根目录）').join('、')}
            </Typography.Text>
          </Form>
        ) : (
          <Form layout="vertical">
            <Form.Item
              label={nodeDialog?.operation === 'rename' ? '新路径'
                : nodeDialog?.operation === 'new-directory' ? '目录路径' : '文件路径'}
              required
            >
              <Input
                aria-label={nodeDialog?.operation === 'rename' ? '新路径'
                  : nodeDialog?.operation === 'new-directory' ? '目录路径' : '文件路径'}
                maxLength={1024}
                value={nodePathValue}
                onChange={(event) => setNodePathValue(event.target.value)}
              />
            </Form.Item>
          </Form>
        )}
      </Modal>

      <Modal
        title={pendingVersionOperation ? '发布操作尚未核验' : '当前草稿尚未保存'}
        open={logoutConfirmationOpen}
        onOk={() => answerLogoutConfirmation(!pendingVersionOperation)}
        onCancel={() => answerLogoutConfirmation(false)}
        okText={pendingVersionOperation ? '返回并继续核验' : '放弃修改并继续'}
        cancelText={pendingVersionOperation ? '留在当前会话' : '继续编辑'}
      >
        {pendingVersionOperation
          ? '发布操作结果尚未确认。请先核验原请求或使用原请求号重试；此时不能切换身份会话。'
          : '继续会切换或清除当前身份会话，本地未保存内容将丢失。是否确认？'}
      </Modal>

      <Modal
        title={pendingVersionOperation ? '发布操作尚未核验' : '离开编辑页？'}
        open={blocker.state === 'blocked'}
        onOk={() => {
          if (!pendingVersionOperation) blocker.proceed?.()
        }}
        onCancel={() => blocker.reset?.()}
        okButtonProps={{ disabled: pendingVersionOperation }}
        okText="离开并放弃本地修改"
        cancelText={pendingVersionOperation ? '返回核验' : '继续编辑'}
      >
        {pendingVersionOperation
          ? '发布结果未知；先核验原请求回执或使用原请求号重试。'
          : '当前草稿尚未保存，离开会丢弃本地修改。'}
      </Modal>
    </Space>
  )
}
