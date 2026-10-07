import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Spin,
  Typography,
} from 'antd'
import { useMemo, useState } from 'react'
import { DdcApiError } from '../api/client'
import {
  ddcAiQueryKeys,
  getAiResource,
  getAiVersion,
  pageAiPublications,
  pageAiResources,
  promoteAiVersion,
} from '../api/ai'
import type {
  DdcAiContent,
  DdcAiPromotionCommand,
  DdcAiPublicationResult,
  DdcAiResourcePageParams,
  DdcAiResourceSummary,
  DdcAiScope,
  DdcAiVersionSummary,
} from '../api/types'
import ScopeSelects from '../components/scope/ScopeSelects'
import { uuidV7 } from '../lib/uuid'

type Props = {
  open: boolean
  sourceResource: DdcAiResourceSummary
  sourceVersion: DdcAiVersionSummary
  onCancel: () => void
  onPublished: (result: DdcAiPublicationResult) => void
  onPendingChange?: (pending: boolean) => void
}

type PendingPromotion = Readonly<{
  requestId: string
  command: Readonly<DdcAiPromotionCommand>
}>

const emptyScope: DdcAiScope = { bizCode: '', namespaceCode: '', env: '', appCode: '' }

const scopeIsComplete = (scope: DdcAiScope): boolean =>
  Object.values(scope).every((value) => value.trim() !== '')

const sameContent = (left: DdcAiContent, right: DdcAiContent): boolean => {
  const normalize = (content: DdcAiContent) => JSON.stringify({
    promptText: content.promptText,
    nodes: content.nodes.slice().sort((a, b) => a.path.localeCompare(b.path)),
  })
  return normalize(left) === normalize(right)
}

const failureMessage = (error: unknown): string =>
  error instanceof Error ? error.message : String(error)

const isUnknownCommit = (error: unknown): boolean =>
  error instanceof DdcApiError && (error.status === 0 || error.status === 503)

const findTargetResource = async (
  scope: DdcAiScope,
  kind: DdcAiResourceSummary['kind'],
  resourceKey: string,
  signal?: AbortSignal,
): Promise<DdcAiResourceSummary | null> => {
  const query: DdcAiResourcePageParams = {
    ...scope,
    kind,
    resourceKeyPrefix: resourceKey,
    pageNo: 1,
    pageSize: 100,
  }
  let pageNo = 1
  let hasNext = true
  while (hasNext) {
    const result = await pageAiResources({ ...query, pageNo }, { signal })
    const exact = result.records.find((record) => record.resourceKey === resourceKey)
    if (exact) return exact
    hasNext = result.page.hasNext
    pageNo += 1
  }
  return null
}

export default function AiPromotionDialog({
  open,
  sourceResource,
  sourceVersion,
  onCancel,
  onPublished,
  onPendingChange = () => {},
}: Props) {
  const { message } = App.useApp()
  const [targetScope, setTargetScope] = useState<DdcAiScope>({ ...emptyScope })
  const [targetResourceKey, setTargetResourceKey] = useState(sourceResource.resourceKey)
  const [changeNote, setChangeNote] = useState(`推广版本 ${sourceVersion.versionNo}`)
  const [failure, setFailure] = useState<string>()
  const [pendingPromotion, setPendingPromotion] = useState<PendingPromotion | null>(null)
  const [verifying, setVerifying] = useState(false)

  const targetQuery = useMemo<DdcAiResourcePageParams | null>(() => {
    if (!scopeIsComplete(targetScope) || targetResourceKey.trim() === '') return null
    return {
      ...targetScope,
      kind: sourceResource.kind,
      resourceKeyPrefix: targetResourceKey.trim(),
      pageNo: 1,
      pageSize: 100,
    }
  }, [sourceResource.kind, targetResourceKey, targetScope])

  const targetIdentityMatchesSource = scopeIsComplete(targetScope)
    && targetScope.bizCode.trim() === sourceResource.scope.bizCode
    && targetScope.namespaceCode.trim() === sourceResource.scope.namespaceCode
    && targetScope.env.trim() === sourceResource.scope.env
    && targetScope.appCode.trim() === sourceResource.scope.appCode
    && targetResourceKey.trim() === sourceResource.resourceKey

  const targetLookupQuery = useQuery({
    queryKey: targetQuery
      ? ddcAiQueryKeys.resources(targetQuery)
      : [...ddcAiQueryKeys.all(), 'promotion-target', sourceResource.id, sourceVersion.versionNo],
    queryFn: ({ signal }) => findTargetResource(
      targetScope,
      sourceResource.kind,
      targetResourceKey.trim(),
      signal,
    ),
    enabled: open && targetQuery !== null && !targetIdentityMatchesSource && pendingPromotion === null,
  })

  const targetResource = targetLookupQuery.data ?? null
  const targetResourceQuery = useQuery({
    queryKey: targetResource
      ? ddcAiQueryKeys.resource(targetResource.id)
      : [...ddcAiQueryKeys.all(), 'promotion-target-detail', sourceResource.id, sourceVersion.versionNo],
    queryFn: ({ signal }) => getAiResource(targetResource!.id, { signal }),
    enabled: open && targetResource !== null && pendingPromotion === null,
  })
  const targetCurrentVersionNo = targetResourceQuery.data?.resource.currentVersionNo ?? null
  const targetVersionQuery = useQuery({
    queryKey: targetCurrentVersionNo
      ? ddcAiQueryKeys.version(targetResource!.id, targetCurrentVersionNo)
      : [...ddcAiQueryKeys.all(), 'promotion-target-version', targetResource?.id ?? 'none'],
    queryFn: ({ signal }) => getAiVersion(targetResource!.id, targetCurrentVersionNo!, { signal }),
    enabled: open && targetResource !== null && targetCurrentVersionNo !== null && pendingPromotion === null,
  })

  const targetPreviewError = targetLookupQuery.error
    ?? targetResourceQuery.error
    ?? targetVersionQuery.error
  const targetPreviewLoading = targetLookupQuery.isFetching
    || (targetResource !== null && targetResourceQuery.isFetching)
    || (targetCurrentVersionNo !== null && targetVersionQuery.isFetching)
  const targetResourceResult = targetResourceQuery.data
  const targetHasUnpublishedChanges = targetResourceResult && targetVersionQuery.data
    ? targetResourceResult.resource.displayName !== targetVersionQuery.data.version.displayName
      || targetResourceResult.resource.description !== targetVersionQuery.data.version.description
      || !sameContent(targetResourceResult.draft, targetVersionQuery.data.content)
    : false

  const promotionMutation = useMutation({
    mutationFn: (request: PendingPromotion) => promoteAiVersion(
      sourceResource.id,
      request.requestId,
      request.command,
    ),
    onSuccess: (result) => {
      setPendingPromotion(null)
      onPendingChange(false)
      setFailure(undefined)
      message.success(`已复制发布目标版本 ${result.targetVersionNo}`)
      onPublished(result)
    },
    onError: (error: unknown) => {
      setFailure(isUnknownCommit(error)
        ? '提交结果未知。请先核验原请求回执；重试时会复用相同请求号和内容。'
        : failureMessage(error))
      if (isUnknownCommit(error)) {
        onPendingChange(true)
      } else {
        setPendingPromotion(null)
        onPendingChange(false)
      }
    },
  })

  const sameTarget = targetIdentityMatchesSource
  const targetLookupComplete = targetLookupQuery.isSuccess
  const targetSnapshotReady = targetResource === null
    || (targetResourceResult !== undefined
      && (targetCurrentVersionNo === null || targetVersionQuery.data !== undefined))
  const canSubmit = open
    && targetQuery !== null
    && !sameTarget
    && targetLookupComplete
    && !targetPreviewError
    && !targetPreviewLoading
    && targetSnapshotReady
    && pendingPromotion === null
    && !promotionMutation.isPending

  const command = (): DdcAiPromotionCommand => ({
    sourceVersionNo: sourceVersion.versionNo,
    targetScope: {
      bizCode: targetScope.bizCode.trim(),
      namespaceCode: targetScope.namespaceCode.trim(),
      env: targetScope.env.trim(),
      appCode: targetScope.appCode.trim(),
    },
    targetResourceKey: targetResourceKey.trim(),
    expectedTargetRevision: targetResourceResult?.resource.revision ?? null,
    changeNote,
  })

  const submit = () => {
    if (!canSubmit) return
    setFailure(undefined)
    const request: PendingPromotion = pendingPromotion ?? {
      requestId: uuidV7(),
      command: command(),
    }
    setPendingPromotion(request)
    onPendingChange(true)
    promotionMutation.mutate(request)
  }

  const verifyOriginalRequest = async () => {
    if (!pendingPromotion || !targetQuery) return
    setVerifying(true)
    setFailure(undefined)
    try {
      const currentTarget = await findTargetResource(
        targetScope,
        sourceResource.kind,
        targetResourceKey.trim(),
      )
      if (!currentTarget) {
        setFailure('未找到发布回执或目标资源；可使用原请求号重试。')
        return
      }
      const audit = await pageAiPublications(currentTarget.id, {
        requestId: pendingPromotion.requestId,
        pageNo: 1,
        pageSize: 20,
      })
      const receipt = audit.records.find((record) =>
        record.operation === 'PROMOTE' && record.sourceVersionId === sourceVersion.id)
      if (!receipt) {
        setFailure('未找到发布回执；可使用原请求号和相同内容重试。')
        return
      }
      setPendingPromotion(null)
      onPendingChange(false)
      message.success(`已核验目标版本 ${receipt.targetVersionNo}`)
      onPublished(receipt)
    } catch (error) {
      setFailure(`核验请求失败：${failureMessage(error)}`)
    } finally {
      setVerifying(false)
    }
  }

  return (
    <Modal
      title={`复制发布固定来源版本 ${sourceVersion.versionNo}`}
      open={open}
      onCancel={() => {
        if (!pendingPromotion) onCancel()
      }}
      onOk={pendingPromotion ? () => promotionMutation.mutate(pendingPromotion) : submit}
      okText={pendingPromotion ? '使用原请求号重试' : '复制并发布'}
      cancelText="取消"
      confirmLoading={promotionMutation.isPending}
      okButtonProps={{ disabled: pendingPromotion === null && !canSubmit }}
      closable={pendingPromotion === null}
      mask={{ closable: pendingPromotion === null }}
      keyboard={pendingPromotion === null}
      destroyOnHidden={pendingPromotion === null}
    >
      <Space orientation="vertical" size="middle" className="tianshu-ai-page">
        <Descriptions size="small" column={1}>
          <Descriptions.Item label="来源资源">
            {`${sourceResource.scope.bizCode} / ${sourceResource.scope.namespaceCode} / ${sourceResource.scope.env} / ${sourceResource.scope.appCode} / ${sourceResource.resourceKey}`}
          </Descriptions.Item>
          <Descriptions.Item label="固定来源版本">
            {`${sourceVersion.versionNo} · ${sourceVersion.displayName} · ${sourceVersion.contentSha256}`}
          </Descriptions.Item>
        </Descriptions>
        {failure && <Alert type="error" showIcon title="复制发布状态" description={failure} />}
        {pendingPromotion && (
          <Button onClick={() => void verifyOriginalRequest()} loading={verifying} disabled={promotionMutation.isPending}>
            核验原请求
          </Button>
        )}
        <Form layout="vertical">
          <Form.Item label="目标四维范围" required>
            <ScopeSelects
              value={targetScope}
              onChange={setTargetScope}
              disabled={pendingPromotion !== null}
            />
          </Form.Item>
          <Form.Item label="目标资源键" required>
            <Input
              aria-label="目标资源键"
              maxLength={128}
              value={targetResourceKey}
              disabled={pendingPromotion !== null}
              onChange={(event) => setTargetResourceKey(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="变更说明">
            <Input.TextArea
              aria-label="变更说明"
              maxLength={512}
              value={pendingPromotion?.command.changeNote ?? changeNote}
              disabled={pendingPromotion !== null}
              onChange={(event) => setChangeNote(event.target.value)}
            />
          </Form.Item>
        </Form>
        {!scopeIsComplete(targetScope) && <Alert type="info" title="请选择完整的目标业务域、namespace、环境和应用。" />}
        {sameTarget && <Alert type="error" title="来源与目标完整资源身份相同，不能复制发布。" />}
        {targetPreviewLoading && <Spin size="small" description="核验目标资源" />}
        {targetPreviewError && (
          <Alert type="error" title="无法读取目标资源状态" description={failureMessage(targetPreviewError)} />
        )}
        {targetQuery && targetLookupComplete && !targetPreviewError && !targetPreviewLoading && (
          targetResourceResult
            ? <Alert
                type="info"
                title={`目标revision ${targetResourceResult.resource.revision}；当前版本 ${targetResourceResult.resource.currentVersionNo ?? '尚未发布'}`}
                description={targetCurrentVersionNo === null
                  ? '目标尚无已发布版本；推广不会覆盖现有草稿。'
                  : targetHasUnpublishedChanges
                    ? '目标存在未发布草稿；本次推广会保留该草稿。'
                    : '目标草稿与当前发布版本一致；本次推广不会改写草稿。'}
              />
            : <Alert type="info" title="目标资源不存在；将以空revision条件创建目标并发布固定来源版本。" />
        )}
        <Typography.Text type="secondary">
          服务端从不可变来源版本复制内容；页面不下载正文后再上传，来源资源与来源草稿保持不变。
        </Typography.Text>
      </Space>
    </Modal>
  )
}
