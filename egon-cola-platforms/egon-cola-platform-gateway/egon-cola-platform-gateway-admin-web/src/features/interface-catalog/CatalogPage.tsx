import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Switch,
  Tag,
  Tree,
  Typography,
  message,
} from 'antd'
import { useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import type { DataNode } from 'antd/es/tree'
import { gatewayApi } from '../../api/gatewayApi'
import type { CatalogTree } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import { readScopeSearchParams, writeScopeSearchParams } from '../../hooks/scopeSearchParams'
import { useCapability } from '../../app/capabilities'

const toTree = (catalog: CatalogTree, search: string): DataNode[] => {
  const keyword = search.toLowerCase()
  return catalog.businessDomains.map((business) => ({
    key: `b:${business.id}`,
    title: `${business.displayName} (${business.code})`,
    children: business.entityDomains.map((entity) => ({
      key: `e:${entity.id}`,
      title: `${entity.displayName} (${entity.code})`,
      children: entity.interfaceGroups.map((group) => ({
        key: `g:${group.id}`,
        title: (
          <Space>
            {group.displayName}
            <Tag>{group.sourceType}</Tag>
          </Space>
        ),
        children: group.operations
          .filter((operation) =>
            `${operation.operationKey} ${operation.methodIdentity}`.toLowerCase().includes(keyword),
          )
          .map((operation) => ({
            key: `o:${operation.id}`,
            title: <Link to={`/operations/${operation.id}`}>{operation.methodIdentity}</Link>,
          })),
      })),
    })),
  }))
}

export const CatalogPage = () => {
  const { applicationId: routeApplicationId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = readScopeSearchParams(searchParams, ['bizCode', 'namespace', 'env', 'appCode'])
  const [applicationId, setApplicationId] = useState<string>()
  const [search, setSearch] = useState('')
  const [hierarchyOpen, setHierarchyOpen] = useState(false)
  const [operationOpen, setOperationOpen] = useState(false)
  const [hierarchyForm] = Form.useForm()
  const [operationForm] = Form.useForm()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:catalog:write')
  const applications = useQuery({
    queryKey: ['applications', filters],
    queryFn: ({ signal }) => gatewayApi.applications(filters, signal),
  })
  const selected = routeApplicationId
    ?? (applicationId && applications.data?.some((item) => item.id === applicationId)
      ? applicationId
      : undefined)
    ?? applications.data?.[0]?.id
  const catalog = useQuery({
    queryKey: ['catalog', selected],
    queryFn: ({ signal }) => gatewayApi.catalog(selected!, signal),
    enabled: Boolean(selected),
  })
  const tree = useMemo(() => (catalog.data ? toTree(catalog.data, search) : []), [catalog.data, search])
  const groups = useMemo(
    () => catalog.data?.businessDomains.flatMap((business) =>
      business.entityDomains.flatMap((entity) =>
        entity.interfaceGroups.map((group) => ({
          value: group.id,
          label: `${business.code} / ${entity.code} / ${group.code}`,
        })),
      ),
    ) ?? [],
    [catalog.data],
  )
  const createHierarchy = useMutation({
    mutationFn: (values: any) =>
      gatewayApi.createManualInterfaceGroup(selected!, values),
    onSuccess: async () => {
      setHierarchyOpen(false)
      hierarchyForm.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['catalog', selected] })
      void message.success('三级目录已创建')
    },
  })
  const createOperation = useMutation({
    mutationFn: (values: any) =>
      gatewayApi.createManualOperation(values.interfaceGroupId, {
        protocol: values.protocol,
        httpMethod: values.protocol === 'HTTP' ? values.httpMethod : undefined,
        path: values.protocol === 'HTTP' ? values.path : undefined,
        serviceName: values.protocol === 'RPC' ? values.serviceName : undefined,
        fullMethodName: values.protocol === 'RPC' ? values.fullMethodName : undefined,
        providerServiceName: values.providerServiceName,
        group: values.group || 'default',
        version: values.version || '1.0.0',
        transport: values.transport,
        externalAccessible: values.externalAccessible,
        definition: {
          summary: values.summary,
          tags: [],
          requestSchema: {},
          responseSchema: {},
          errorSchema: [],
          attributes: {},
          externalAccessible: values.externalAccessible,
        },
      }),
    onSuccess: async () => {
      setOperationOpen(false)
      operationForm.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['catalog', selected] })
      void message.success('Operation 已创建')
    },
  })
  if (applications.isLoading) return <LoadingBlock />
  if (applications.error) return <QueryFailure error={applications.error} />
  return (
    <section>
      <Space className="page-title">
        <Typography.Title level={2}>接口目录</Typography.Title>
        <Button disabled={!canWrite || !selected} onClick={() => setHierarchyOpen(true)}>
          新建业务域 / 实体域 / 接口组
        </Button>
        <Button
          type="primary"
          disabled={!canWrite || groups.length === 0}
          onClick={() => setOperationOpen(true)}
        >
          新建 Operation
        </Button>
      </Space>
      <Card>
        <Space wrap>
          <GatewayScopeFilter
            fields={['bizCode', 'namespace', 'env', 'appCode']}
            value={filters}
            onChange={(value) => setSearchParams(
              writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode']),
            )}
          />
          <Select
            aria-label="应用"
            style={{ width: 260 }}
            value={selected}
            placeholder="选择应用"
            options={(applications.data ?? []).map((application) => ({
              value: application.id,
              label: `${application.bizCode} / ${application.applicationCode} / ${application.env} / ${application.namespace} · ${application.displayName}`,
            }))}
            onChange={setApplicationId}
          />
          {selected && applications.data?.find((application) => application.id === selected) && (
            <Typography.Text type="secondary">
              Scope：{(() => {
                const item = applications.data!.find((application) => application.id === selected)!
                return `${item.bizCode} / ${item.applicationCode} / ${item.env} / ${item.namespace}`
              })()}
            </Typography.Text>
          )}
          <Input.Search
            aria-label="接口搜索"
            allowClear
            placeholder="名称 / Code / Method / Path / RPC Method"
            onSearch={setSearch}
            onChange={(event) => setSearch(event.target.value)}
            style={{ width: 380 }}
          />
        </Space>
      </Card>
      <Row gutter={16} className="section-row">
        <Col span={24}>
          <Card title="Application → Business Domain → Entity Domain → Interface Group → Operation">
            {catalog.isLoading ? <LoadingBlock /> : catalog.error ? (
              <QueryFailure error={catalog.error} />
            ) : tree.length ? (
              <Tree showLine virtual height={560} treeData={tree} defaultExpandAll />
            ) : (
              <EmptyBlock description="当前作用域没有接口定义" />
            )}
          </Card>
        </Col>
      </Row>
      <Modal
        title="新建三级目录"
        open={hierarchyOpen}
        onCancel={() => setHierarchyOpen(false)}
        onOk={() => hierarchyForm.submit()}
        confirmLoading={createHierarchy.isPending}
        destroyOnHidden
      >
        <Form
          form={hierarchyForm}
          layout="vertical"
          onFinish={(values) => createHierarchy.mutate(values)}
        >
          {[
            ['businessCode', '业务域 Code'],
            ['businessName', '业务域名称'],
            ['entityCode', '实体域 Code'],
            ['entityName', '实体域名称'],
            ['interfaceGroupCode', '接口组 Code'],
            ['interfaceGroupName', '接口组名称'],
          ].map(([name, label]) => (
            <Form.Item key={name} name={name} label={label} rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          ))}
          <Form.Item name="className" label="Controller / Service 类名"><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="新建 Operation"
        open={operationOpen}
        onCancel={() => setOperationOpen(false)}
        onOk={() => operationForm.submit()}
        confirmLoading={createOperation.isPending}
        destroyOnHidden
      >
        <Form
          form={operationForm}
          layout="vertical"
          initialValues={{
            protocol: 'HTTP',
            httpMethod: 'GET',
            group: 'default',
            version: '1.0.0',
            externalAccessible: false,
          }}
          onFinish={(values) => createOperation.mutate(values)}
        >
          <Form.Item name="interfaceGroupId" label="接口组" rules={[{ required: true }]}>
            <Select options={groups} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="protocol" label="协议" rules={[{ required: true }]}>
            <Select options={['HTTP', 'RPC'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(previous, current) => previous.protocol !== current.protocol}>
            {({ getFieldValue }) => getFieldValue('protocol') === 'HTTP' ? (
              <>
                <Form.Item name="httpMethod" label="HTTP Method" rules={[{ required: true }]}>
                  <Select options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((value) => ({ value }))} />
                </Form.Item>
                <Form.Item name="path" label="Path" rules={[{ required: true }]}><Input /></Form.Item>
              </>
            ) : (
              <>
                <Form.Item name="serviceName" label="RPC Service" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="fullMethodName" label="Full Method Name" rules={[{ required: true }]}><Input /></Form.Item>
              </>
            )}
          </Form.Item>
          <Form.Item name="providerServiceName" label="Provider Service" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="group" label="Group"><Input /></Form.Item>
          <Form.Item name="version" label="Version"><Input /></Form.Item>
          <Form.Item name="transport" label="Transport"><Input /></Form.Item>
          <Form.Item name="summary" label="摘要"><Input /></Form.Item>
          <Form.Item name="externalAccessible" label="允许外部调用" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
