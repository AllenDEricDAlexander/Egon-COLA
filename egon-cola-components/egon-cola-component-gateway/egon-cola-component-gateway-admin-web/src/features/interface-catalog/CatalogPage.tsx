import { useQuery } from '@tanstack/react-query'
import { Card, Col, Input, Row, Select, Space, Tag, Tree, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { DataNode } from 'antd/es/tree'
import { gatewayApi } from '../../api/gatewayApi'
import type { CatalogTree } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'

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
  const { scope } = useScope()
  const [applicationId, setApplicationId] = useState<string>()
  const [search, setSearch] = useState('')
  const applications = useQuery({
    queryKey: ['applications', scope],
    queryFn: ({ signal }) => gatewayApi.applications(scope, signal),
  })
  const selected = applicationId ?? applications.data?.[0]?.id
  const catalog = useQuery({
    queryKey: ['catalog', selected],
    queryFn: ({ signal }) => gatewayApi.catalog(selected!, signal),
    enabled: Boolean(selected),
  })
  const tree = useMemo(() => (catalog.data ? toTree(catalog.data, search) : []), [catalog.data, search])
  if (applications.isLoading) return <LoadingBlock />
  if (applications.error) return <QueryFailure error={applications.error} />
  return (
    <section>
      <Typography.Title level={2}>接口目录</Typography.Title>
      <Card>
        <Space wrap>
          <Select
            aria-label="应用"
            style={{ width: 260 }}
            value={selected}
            placeholder="选择应用"
            options={(applications.data ?? []).map((application) => ({
              value: application.id,
              label: `${application.displayName} (${application.applicationCode})`,
            }))}
            onChange={setApplicationId}
          />
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
    </section>
  )
}
