import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Form, Input, Modal, Select, Space, Table, Typography} from 'antd'
import {useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {
  knowledgeApi,
  knowledgeEgressPolicyValues,
  type KnowledgeBase,
  type KnowledgeBaseCommand,
} from '../../api/knowledge'
import {GatewayApiError} from '../../api/client'
import {useCapability} from '../../app/capabilities'
import {EmptyBlock, LoadingBlock, QueryFailure} from '../../components/QueryState'

type CreateForm = KnowledgeBaseCommand

// 员工只能列出自己作为成员的 KB（服务端过滤）；命令体不含 actorId/tenantId。
export const KnowledgeBasesPage = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const canCreate = useCapability('yuheng:knowledge:write')
  const [name, setName] = useState('')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm<CreateForm>()
  const bases = useQuery({
    queryKey: ['knowledge', 'bases', {name}],
    queryFn: ({signal}) => knowledgeApi.knowledgeBases({name: name || undefined}, signal),
  })
  const create = useMutation({
    mutationFn: (command: CreateForm) => knowledgeApi.createKnowledgeBase(command),
    retry: false,
    onSuccess: async (created) => {
      await queryClient.invalidateQueries({queryKey: ['knowledge', 'bases']})
      setOpen(false)
      form.resetFields()
      navigate(`/knowledge/${created.id}`)
    },
  })

  if (bases.isLoading) return <LoadingBlock />
  if (bases.isError) {
    // 无权限与“有权限但无 KB”必须可区分：403 由错误分支给出，空列表走空态。
    return <QueryFailure error={bases.error} retry={() => void bases.refetch()} />
  }

  const items = bases.data?.items ?? []
  const conflict = create.error instanceof GatewayApiError && create.error.status === 409

  return (
    <Card
      title="企业知识"
      extra={(
        <Space>
          <Input.Search
            allowClear
            placeholder="按名称检索"
            aria-label="知识库名称"
            onChange={(event) => setName(event.target.value)}
            onSearch={(value) => setName(value)}
          />
          <Button
            type="primary"
            disabled={!canCreate}
            title={canCreate ? undefined : '当前账号缺少 yuheng:knowledge:write 能力'}
            onClick={() => setOpen(true)}
          >
            新建知识库
          </Button>
        </Space>
      )}
    >
      {items.length === 0 ? (
        <EmptyBlock description="你还没有被加入任何知识库，请联系知识库所有者把你加为成员" />
      ) : (
        <Table<KnowledgeBase>
          rowKey="id"
          dataSource={items}
          pagination={{total: bases.data?.total ?? 0, pageSize: 20}}
          columns={[
            {
              title: '名称',
              dataIndex: 'name',
              render: (_, record) => (
                <Typography.Link onClick={() => navigate(`/knowledge/${record.id}`)}>
                  {record.name}
                </Typography.Link>
              ),
            },
            {title: '我的角色', dataIndex: 'myRole'},
            {title: '出域策略', dataIndex: 'egressPolicy'},
            {title: '对话模型', dataIndex: 'chatModel'},
            {title: '嵌入模型', dataIndex: 'embeddingModel'},
          ]}
        />
      )}
      <Modal
        open={open}
        title="新建知识库"
        okText="创建"
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={create.isPending}
        destroyOnHidden
      >
        {create.isError && (
          <Alert
            type="error"
            showIcon
            message={create.error instanceof Error ? create.error.message : '创建失败'}
            description={conflict ? '已有同名意图被拒绝，请调整名称后重试' : undefined}
          />
        )}
        <Form<CreateForm>
          form={form}
          layout="vertical"
          onFinish={(values) => create.mutate(values)}
          initialValues={{egressPolicy: 'LOCAL_ONLY', description: ''}}
        >
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[{required: true, message: '请填写名称'}, {max: 128, message: '名称过长'}]}
          >
            <Input aria-label="知识库名称字段" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea aria-label="知识库说明" rows={3} maxLength={2000} showCount />
          </Form.Item>
          <Form.Item name="egressPolicy" label="出域策略" rules={[{required: true}]}>
            <Select
              aria-label="出域策略"
              options={knowledgeEgressPolicyValues.map((value) => ({value, label: value}))}
            />
          </Form.Item>
          <Form.Item
            name="chatModel"
            label="对话模型 alias"
            rules={[{required: true, message: '请选择对话模型'}]}
          >
            <Input aria-label="对话模型" />
          </Form.Item>
          <Form.Item
            name="embeddingModel"
            label="嵌入模型 alias"
            extra="嵌入始终走本地模型，不回落云端。"
            rules={[{required: true, message: '请选择嵌入模型'}]}
          >
            <Input aria-label="嵌入模型" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
