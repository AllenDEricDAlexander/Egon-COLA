import {PermissionGuard, useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tabs, Tag, Typography} from 'antd'
import {useState} from 'react'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {
  constraintApi,
  type CardinalityCommand,
  type DataRuleCommand,
  type DataRuleView,
  type FieldRuleCommand,
  type FieldRuleView,
  type OperationSodRuleCommand,
  type OperationSodRuleView,
  type PrerequisiteGroupCommand,
  type SodSetCommand,
  type SodSetView,
} from './constraint.api'

export interface DsdRoleSelection {
  readonly roleId: string
  readonly applicationId: string
  readonly roleType: string
}

export const validateDsdRoleSelection = (
  roles: readonly DsdRoleSelection[],
): string | null => {
  if (roles.some((role) => role.roleType !== 'ACTIVATION_ROOT')) {
    return 'DSD_ROLE_MUST_BE_ACTIVATION_ROOT'
  }
  if (new Set(roles.map((role) => role.applicationId)).size > 1) {
    return 'DSD_ROLES_MUST_SHARE_APPLICATION'
  }
  return null
}

type RuleKind = 'sod' | 'data' | 'field' | 'operation'
type ConstraintRow = SodSetView | DataRuleView | FieldRuleView | OperationSodRuleView

interface ConstraintFormValues {
  readonly setCode?: string
  readonly constraintType?: string
  readonly applicationId?: string
  readonly maximumActiveRoles?: number
  readonly memberRoleIds?: string
  readonly validFrom?: string
  readonly validTo?: string
  readonly roleId?: string
  readonly groupCode?: string
  readonly matchMode?: string
  readonly prerequisiteRoleIds?: string
  readonly expectedRoleVersion?: number
  readonly scopeType?: string
  readonly maximumActive?: number
  readonly expectedVersion?: number
  readonly directorySnapshotVersion?: number
  readonly references?: string
  readonly permissionId?: string
  readonly fieldDefinitionId?: string
  readonly accessLevel?: string
  readonly applicationCode?: string
  readonly businessResource?: string
  readonly priorActionCode?: string
  readonly forbiddenLaterActionCode?: string
  readonly lookbackFrom?: string
}

interface EditorState {
  readonly kind: RuleKind
  readonly rowId: string | null
  readonly initialValues: ConstraintFormValues
}

interface RuleSaveInput {
  readonly kind: RuleKind
  readonly rowId: string | null
  readonly values: ConstraintFormValues
}

interface PrerequisiteFormValues {
  readonly roleId: string
  readonly groupCode: string
  readonly matchMode: string
  readonly prerequisiteRoleIds: string
  readonly expectedRoleVersion: number
}

interface CardinalityFormValues {
  readonly roleId: string
  readonly scopeType: string
  readonly maximumActive: number
  readonly validFrom: string
  readonly validTo?: string
  readonly expectedVersion: number
}

const managementPermission: Record<RuleKind, string> = {
  sod: 'system:authorization-constraint:manage',
  data: 'system:data-rule:manage',
  field: 'system:field-rule:manage',
  operation: 'system:operation-sod:manage',
}

const localDateTime = (value?: string | null): string => {
  if (value) return value.slice(0, 16)
  return new Date().toISOString().slice(0, 16)
}

const isoDateTime = (value: string | undefined, fallback: string | null = null): string | null => {
  if (!value) return fallback
  return new Date(value).toISOString()
}

const listValue = (value: string | undefined): string[] => (value ?? '')
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const referencesValue = (value: readonly {readonly referenceType: string; readonly referenceId: string}[]): string => value
  .map((reference) => `${reference.referenceType}:${reference.referenceId}`)
  .join(', ')

const parseReferences = (value: string | undefined) => listValue(value).map((item) => {
  const [referenceType, ...referenceIdParts] = item.split(':')
  return {
    referenceType: referenceType.trim(),
    referenceId: referenceIdParts.join(':').trim(),
  }
})

const referenceValidator = (_rule: unknown, value?: string) => {
  const valid = listValue(value).every((item) => {
    const [referenceType, ...referenceIdParts] = item.split(':')
    return referenceType.trim().length > 0 && referenceIdParts.join(':').trim().length > 0
  })
  return valid ? Promise.resolve() : Promise.reject(new Error('引用必须使用 TYPE:ID 格式'))
}

const initialValues = (kind: RuleKind, row?: ConstraintRow): ConstraintFormValues => {
  const base: ConstraintFormValues = {
    validFrom: localDateTime(),
    expectedVersion: 0,
  }
  if (!row) return base
  if (kind === 'sod') {
    const value = row as SodSetView
    return {
      ...base,
      setCode: value.setCode,
      constraintType: value.constraintType,
      applicationId: value.applicationId ?? '',
      maximumActiveRoles: value.maximumActiveRoles,
      memberRoleIds: value.roleIds.join(', '),
      expectedVersion: value.version,
    }
  }
  if (kind === 'data') {
    const value = row as DataRuleView
    return {
      ...base,
      applicationId: value.applicationId,
      roleId: value.roleId,
      permissionId: value.permissionId,
      scopeType: value.scopeType,
      references: referencesValue(value.references),
      expectedVersion: value.version,
    }
  }
  if (kind === 'field') {
    const value = row as FieldRuleView
    return {
      ...base,
      applicationId: value.applicationId,
      roleId: value.roleId,
      permissionId: value.permissionId,
      fieldDefinitionId: value.fieldDefinitionId,
      accessLevel: value.accessLevel,
      expectedVersion: value.version,
    }
  }
  const value = row as OperationSodRuleView
  return {
    ...base,
    applicationCode: value.applicationCode,
    businessResource: value.businessResource,
    priorActionCode: value.priorActionCode,
    forbiddenLaterActionCode: value.forbiddenLaterActionCode,
    expectedVersion: value.version,
  }
}

const commandFor = (kind: RuleKind, values: ConstraintFormValues): SodSetCommand | DataRuleCommand | FieldRuleCommand | OperationSodRuleCommand => {
  const validFrom = isoDateTime(values.validFrom, new Date().toISOString())!
  const validTo = isoDateTime(values.validTo)
  if (kind === 'sod') {
    return {
      setCode: values.setCode?.trim() ?? '',
      constraintType: values.constraintType ?? 'SSD',
      applicationId: values.applicationId?.trim() || null,
      maximumActiveRoles: values.maximumActiveRoles ?? 1,
      memberRoleIds: listValue(values.memberRoleIds),
      validFrom,
      validTo,
      expectedVersion: values.expectedVersion ?? 0,
    }
  }
  if (kind === 'data') {
    return {
      applicationId: values.applicationId?.trim() ?? '',
      roleId: values.roleId?.trim() ?? '',
      permissionId: values.permissionId?.trim() ?? '',
      scopeType: values.scopeType?.trim() ?? '',
      directorySnapshotVersion: values.directorySnapshotVersion ?? null,
      references: parseReferences(values.references),
      validFrom,
      validTo,
      expectedVersion: values.expectedVersion ?? 0,
    }
  }
  if (kind === 'field') {
    return {
      applicationId: values.applicationId?.trim() ?? '',
      roleId: values.roleId?.trim() ?? '',
      permissionId: values.permissionId?.trim() ?? '',
      fieldDefinitionId: values.fieldDefinitionId?.trim() ?? '',
      accessLevel: values.accessLevel?.trim() ?? '',
      validFrom,
      validTo,
      expectedVersion: values.expectedVersion ?? 0,
    }
  }
  return {
    applicationCode: values.applicationCode?.trim() ?? '',
    businessResource: values.businessResource?.trim() ?? '',
    priorActionCode: values.priorActionCode?.trim() ?? '',
    forbiddenLaterActionCode: values.forbiddenLaterActionCode?.trim() ?? '',
    lookbackFrom: isoDateTime(values.lookbackFrom),
    validFrom,
    validTo,
    expectedVersion: values.expectedVersion ?? 0,
  }
}

export const ConstraintPage = () => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = constraintApi(useFeatureApi())
  const queryClient = useQueryClient()
  const tenant = effectiveTenantId ?? 'none'
  const enabled = status === 'READY'
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [form] = Form.useForm<ConstraintFormValues>()
  const sod = useQuery({queryKey: ['tianquan-jianshen', 'sod', tenant], queryFn: api.sodSets, enabled})
  const data = useQuery({queryKey: ['tianquan-jianshen', 'data-rules', tenant], queryFn: api.dataRules, enabled})
  const fields = useQuery({queryKey: ['tianquan-jianshen', 'field-rules', tenant], queryFn: api.fieldRules, enabled})
  const operations = useQuery({queryKey: ['tianquan-jianshen', 'operation-sod', tenant], queryFn: api.operationSodRules, enabled})
  const save = useMutation({
    mutationFn: ({kind, rowId, values}: RuleSaveInput) => {
      if (kind === 'sod') {
        const command = commandFor(kind, values) as SodSetCommand
        return rowId ? api.updateSod(rowId, command) : api.createSod(command)
      }
      if (kind === 'data') {
        const command = commandFor(kind, values) as DataRuleCommand
        return rowId ? api.updateDataRule(rowId, command) : api.createDataRule(command)
      }
      if (kind === 'field') {
        const command = commandFor(kind, values) as FieldRuleCommand
        return rowId ? api.updateFieldRule(rowId, command) : api.createFieldRule(command)
      }
      const command = commandFor(kind, values) as OperationSodRuleCommand
      return rowId ? api.updateOperationSodRule(rowId, command) : api.createOperationSodRule(command)
    },
    onSuccess: async () => {
      setEditor(null)
      await queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'sod', tenant]})
      await queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'data-rules', tenant]})
      await queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'field-rules', tenant]})
      await queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'operation-sod', tenant]})
    },
  })
  const savePrerequisites = useMutation({
    mutationFn: (values: PrerequisiteFormValues) => api.savePrerequisites(values.roleId.trim(), {
      groupCode: values.groupCode.trim(),
      matchMode: values.matchMode,
      prerequisiteRoleIds: listValue(values.prerequisiteRoleIds),
      expectedRoleVersion: values.expectedRoleVersion,
    } satisfies PrerequisiteGroupCommand),
    onSuccess: () => queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'about']}),
  })
  const saveCardinality = useMutation({
    mutationFn: (values: CardinalityFormValues) => api.saveCardinality(values.roleId.trim(), {
      scopeType: values.scopeType.trim(),
      maximumActive: values.maximumActive,
      validFrom: isoDateTime(values.validFrom, new Date().toISOString())!,
      validTo: isoDateTime(values.validTo),
      expectedVersion: values.expectedVersion,
    } satisfies CardinalityCommand),
    onSuccess: () => queryClient.invalidateQueries({queryKey: ['tianquan-jianshen', 'about']}),
  })
  const mutationError = save.error ?? savePrerequisites.error ?? saveCardinality.error
  const openEditor = (kind: RuleKind, row?: ConstraintRow) => {
    setEditor({kind, rowId: row ? rowId(kind, row) : null, initialValues: initialValues(kind, row)})
  }

  return (
    <Card title="授权约束">
      <Typography.Paragraph type="secondary">
        DSD 只引用同一 APP 的激活根角色，并在会话激活角色集合原子替换时校验；SSD、Prerequisite 与 Cardinality 在分配时校验。
      </Typography.Paragraph>
      {mutationError && (
        <Alert
          type="error"
          showIcon
          title={mutationError instanceof Error ? mutationError.message : '授权约束保存失败'}
          action={<Button size="small" onClick={() => {void sod.refetch(); void data.refetch(); void fields.refetch(); void operations.refetch()}}>刷新</Button>}
          style={{marginBottom: 16}}
        />
      )}
      <Tabs items={[
        {
          key: 'sod',
          label: `SSD / DSD (${sod.data?.length ?? 0})`,
          children: (
            <PageState loading={sod.isPending} error={sod.error} empty={sod.data?.length === 0} onRetry={() => {void sod.refetch()}}>
              <Table<SodSetView>
                rowKey="setId"
                dataSource={sod.data ?? []}
                pagination={false}
                scroll={{x: 'max-content'}}
                title={() => (
                  <PermissionGuard permission={managementPermission.sod}>
                    <Button type="primary" onClick={() => openEditor('sod')}>新增 SSD / DSD 集合</Button>
                  </PermissionGuard>
                )}
                columns={[
                  {title: '集合编码', dataIndex: 'setCode'},
                  {title: '类型', dataIndex: 'constraintType', render: (value: string) => <Tag>{value}</Tag>},
                  {title: 'APP', dataIndex: 'applicationId'},
                  {title: '最大同时激活', dataIndex: 'maximumActiveRoles'},
                  {title: '激活根角色', dataIndex: 'roleIds', render: (ids: readonly string[]) => ids.join(', ')},
                  {title: '状态', dataIndex: 'status'},
                  {title: '版本', dataIndex: 'version'},
                  {title: '操作', render: (_value: unknown, row: SodSetView) => <PermissionGuard permission={managementPermission.sod}><Button size="small" onClick={() => openEditor('sod', row)}>编辑</Button></PermissionGuard>},
                ]}
              />
            </PageState>
          ),
        },
        {
          key: 'data',
          label: `数据规则 (${data.data?.length ?? 0})`,
          children: (
            <PageState loading={data.isPending} error={data.error} empty={data.data?.length === 0} onRetry={() => {void data.refetch()}}>
              <Table<DataRuleView>
                rowKey="ruleId"
                dataSource={data.data ?? []}
                pagination={false}
                scroll={{x: 'max-content'}}
                title={() => <PermissionGuard permission={managementPermission.data}><Button type="primary" onClick={() => openEditor('data')}>新增数据规则</Button></PermissionGuard>}
                columns={[
                  {title: '应用', dataIndex: 'applicationId'},
                  {title: '角色', dataIndex: 'roleId'},
                  {title: '权限', dataIndex: 'permissionId'},
                  {title: '范围类型', dataIndex: 'scopeType'},
                  {title: '引用', dataIndex: 'references', render: (references: DataRuleView['references']) => referencesValue(references)},
                  {title: '状态', dataIndex: 'status'},
                  {title: '版本', dataIndex: 'version'},
                  {title: '操作', render: (_value: unknown, row: DataRuleView) => <PermissionGuard permission={managementPermission.data}><Button size="small" onClick={() => openEditor('data', row)}>编辑</Button></PermissionGuard>},
                ]}
              />
            </PageState>
          ),
        },
        {
          key: 'field',
          label: `字段规则 (${fields.data?.length ?? 0})`,
          children: (
            <PageState loading={fields.isPending} error={fields.error} empty={fields.data?.length === 0} onRetry={() => {void fields.refetch()}}>
              <Table<FieldRuleView>
                rowKey="ruleId"
                dataSource={fields.data ?? []}
                pagination={false}
                scroll={{x: 'max-content'}}
                title={() => <PermissionGuard permission={managementPermission.field}><Button type="primary" onClick={() => openEditor('field')}>新增字段规则</Button></PermissionGuard>}
                columns={[
                  {title: '应用', dataIndex: 'applicationId'},
                  {title: '角色', dataIndex: 'roleId'},
                  {title: '权限', dataIndex: 'permissionId'},
                  {title: '字段', dataIndex: 'fieldDefinitionId'},
                  {title: '访问级别', dataIndex: 'accessLevel'},
                  {title: '状态', dataIndex: 'status'},
                  {title: '版本', dataIndex: 'version'},
                  {title: '操作', render: (_value: unknown, row: FieldRuleView) => <PermissionGuard permission={managementPermission.field}><Button size="small" onClick={() => openEditor('field', row)}>编辑</Button></PermissionGuard>},
                ]}
              />
            </PageState>
          ),
        },
        {
          key: 'operation',
          label: `Operation SOD (${operations.data?.length ?? 0})`,
          children: (
            <PageState loading={operations.isPending} error={operations.error} empty={operations.data?.length === 0} onRetry={() => {void operations.refetch()}}>
              <Table<OperationSodRuleView>
                rowKey="ruleId"
                dataSource={operations.data ?? []}
                pagination={false}
                scroll={{x: 'max-content'}}
                title={() => <PermissionGuard permission={managementPermission.operation}><Button type="primary" onClick={() => openEditor('operation')}>新增 Operation SOD</Button></PermissionGuard>}
                columns={[
                  {title: '应用编码', dataIndex: 'applicationCode'},
                  {title: '业务对象', dataIndex: 'businessResource'},
                  {title: '前置动作', dataIndex: 'priorActionCode'},
                  {title: '禁止后置动作', dataIndex: 'forbiddenLaterActionCode'},
                  {title: '状态', dataIndex: 'status'},
                  {title: '版本', dataIndex: 'version'},
                  {title: '操作', render: (_value: unknown, row: OperationSodRuleView) => <PermissionGuard permission={managementPermission.operation}><Button size="small" onClick={() => openEditor('operation', row)}>编辑</Button></PermissionGuard>},
                ]}
              />
            </PageState>
          ),
        },
      ]} />
      <Card size="small" title="角色级约束配置" style={{marginTop: 16}}>
        <Typography.Paragraph type="secondary">
          前置条件组和角色容量没有独立列表接口，按角色 ID 直接替换服务端配置；版本号用于防止覆盖其他管理员的修改。
        </Typography.Paragraph>
        <Space orientation="vertical" size="middle" style={{width: '100%'}}>
          <PermissionGuard permission="system:authorization-constraint:manage">
            <Form<PrerequisiteFormValues>
              layout="inline"
              onFinish={(values) => savePrerequisites.mutate(values)}
              initialValues={{matchMode: 'ALL', expectedRoleVersion: 0}}
            >
              <Form.Item name="roleId" rules={[{required: true, whitespace: true}]}><Input placeholder="目标角色 ID" /></Form.Item>
              <Form.Item name="groupCode" rules={[{required: true, whitespace: true}]}><Input placeholder="前置组编码" /></Form.Item>
              <Form.Item name="matchMode" rules={[{required: true}]}><Select style={{width: 120}} options={['ALL', 'ANY'].map((value) => ({value, label: value}))} /></Form.Item>
              <Form.Item name="prerequisiteRoleIds" rules={[{required: true, whitespace: true}]}><Input placeholder="前置角色 ID，逗号分隔" /></Form.Item>
              <Form.Item name="expectedRoleVersion" rules={[{required: true}]}><InputNumber min={0} precision={0} placeholder="角色版本" /></Form.Item>
              <Button type="primary" htmlType="submit" loading={savePrerequisites.isPending}>保存前置条件</Button>
            </Form>
          </PermissionGuard>
          <PermissionGuard permission="system:authorization-constraint:manage">
            <Form<CardinalityFormValues>
              layout="inline"
              onFinish={(values) => saveCardinality.mutate(values)}
              initialValues={{validFrom: localDateTime(), expectedVersion: 0, maximumActive: 1, scopeType: 'TENANT'}}
            >
              <Form.Item name="roleId" rules={[{required: true, whitespace: true}]}><Input placeholder="目标角色 ID" /></Form.Item>
              <Form.Item name="scopeType" rules={[{required: true, whitespace: true}]}><Input placeholder="容量范围类型" /></Form.Item>
              <Form.Item name="maximumActive" rules={[{required: true}]}><InputNumber min={1} precision={0} placeholder="最大激活数" /></Form.Item>
              <Form.Item name="validFrom" rules={[{required: true}]}><Input type="datetime-local" /></Form.Item>
              <Form.Item name="validTo"><Input type="datetime-local" /></Form.Item>
              <Form.Item name="expectedVersion" rules={[{required: true}]}><InputNumber min={0} precision={0} placeholder="策略版本" /></Form.Item>
              <Button type="primary" htmlType="submit" loading={saveCardinality.isPending}>保存角色容量</Button>
            </Form>
          </PermissionGuard>
        </Space>
      </Card>
      {editor && (
        <Modal
          open
          title={`${editor.rowId ? '编辑' : '新增'}${ruleTitle(editor.kind)}`}
          onCancel={() => setEditor(null)}
          onOk={() => {void form.submit()}}
          okText="保存"
          confirmLoading={save.isPending}
          destroyOnHidden
          width={720}
        >
          <Form<ConstraintFormValues>
            key={`${editor.kind}-${editor.rowId ?? 'new'}`}
            form={form}
            layout="vertical"
            initialValues={editor.initialValues}
            onFinish={(values) => save.mutate({kind: editor.kind, rowId: editor.rowId, values})}
          >
            {editor.kind === 'sod' && <>
              <Form.Item name="setCode" label="集合编码" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="constraintType" label="约束类型" rules={[{required: true}]}><Select options={['SSD', 'DSD'].map((value) => ({value, label: value}))} /></Form.Item>
              <Form.Item name="applicationId" label="应用 ID"><Input /></Form.Item>
              <Form.Item name="maximumActiveRoles" label="最大同时激活角色数" rules={[{required: true}]}><InputNumber min={1} precision={0} style={{width: '100%'}} /></Form.Item>
              <Form.Item name="memberRoleIds" label="成员角色 ID（逗号分隔）" rules={[{required: true, whitespace: true}]}><Input.TextArea /></Form.Item>
            </>}
            {editor.kind === 'data' && <>
              <Form.Item name="applicationId" label="应用 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="roleId" label="角色 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="permissionId" label="权限 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="scopeType" label="范围类型" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="directorySnapshotVersion" label="目录快照版本"><InputNumber min={0} precision={0} style={{width: '100%'}} /></Form.Item>
              <Form.Item name="references" label="范围引用（TYPE:ID，逗号分隔）" rules={[{required: true, whitespace: true}, {validator: referenceValidator}]}><Input.TextArea /></Form.Item>
            </>}
            {editor.kind === 'field' && <>
              <Form.Item name="applicationId" label="应用 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="roleId" label="角色 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="permissionId" label="权限 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="fieldDefinitionId" label="字段定义 ID" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="accessLevel" label="访问级别" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
            </>}
            {editor.kind === 'operation' && <>
              <Form.Item name="applicationCode" label="应用编码" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="businessResource" label="业务对象" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="priorActionCode" label="前置动作" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="forbiddenLaterActionCode" label="禁止后置动作" rules={[{required: true, whitespace: true}]}><Input /></Form.Item>
              <Form.Item name="lookbackFrom" label="回溯起点"><Input type="datetime-local" /></Form.Item>
            </>}
            <Form.Item name="validFrom" label="生效时间" rules={[{required: true}]}><Input type="datetime-local" /></Form.Item>
            <Form.Item name="validTo" label="失效时间"><Input type="datetime-local" /></Form.Item>
            <Form.Item name="expectedVersion" label="期望版本" rules={[{required: true}]}><InputNumber min={0} precision={0} style={{width: '100%'}} /></Form.Item>
          </Form>
        </Modal>
      )}
    </Card>
  )
}

const rowId = (kind: RuleKind, row: ConstraintRow): string => {
  if (kind === 'sod') return (row as SodSetView).setId
  return (row as DataRuleView | FieldRuleView | OperationSodRuleView).ruleId
}

const ruleTitle = (kind: RuleKind): string => ({
  sod: 'SSD / DSD 集合',
  data: '数据规则',
  field: '字段规则',
  operation: 'Operation SOD 规则',
}[kind])
