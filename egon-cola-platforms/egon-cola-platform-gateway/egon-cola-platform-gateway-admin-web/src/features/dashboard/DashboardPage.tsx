import { Line, Pie } from '@ant-design/charts'
import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic, Table, Typography } from 'antd'
import { gatewayApi } from '../../api/gatewayApi'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useScope } from '../../hooks/useScope'

export const DashboardPage = () => {
  const { scope } = useScope()
  const query = useQuery({
    queryKey: ['dashboard', scope],
    queryFn: ({ signal }) => gatewayApi.dashboard(scope, signal),
  })
  if (query.isLoading) return <LoadingBlock />
  if (query.error || !query.data) {
    return <QueryFailure error={query.error} retry={() => void query.refetch()} />
  }
  const data = query.data
  const series = data.requestSeries.flatMap((point) => [
    { time: point.time, metric: '请求量', value: point.requests },
    { time: point.time, metric: '错误量', value: point.errors },
  ])
  return (
    <section>
      <Typography.Title level={2}>运行总览</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="Gateway Group" value={data.gatewayGroups} /></Card></Col>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="Ready Engine" value={data.readyEngines} suffix={`/ ${data.totalEngines}`} /></Card></Col>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="版本不一致 Group" value={data.inconsistentGroups} /></Card></Col>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="活跃 Provider" value={data.activeProviders} /></Card></Col>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="异常 Provider" value={data.abnormalProviders} /></Card></Col>
        <Col xs={24} md={8} xl={4}><Card><Statistic title="发布成功率" value={data.releaseSuccessRate * 100} precision={1} suffix="%" /></Card></Col>
      </Row>
      <Row gutter={[16, 16]} className="section-row">
        <Col xs={24} xl={16}>
          <Card title="请求量与错误量">
            <Line data={series} xField="time" yField="value" colorField="metric" height={280} />
            <Typography.Paragraph type="secondary">
              同等信息表格：每个时间点均列出请求量与错误量。
            </Typography.Paragraph>
            <Table
              size="small"
              pagination={false}
              rowKey="time"
              dataSource={data.requestSeries}
              columns={[
                { title: '时间', dataIndex: 'time' },
                { title: '请求', dataIndex: 'requests' },
                { title: '错误', dataIndex: 'errors' },
                { title: 'P50(ms)', dataIndex: 'p50' },
                { title: 'P95(ms)', dataIndex: 'p95' },
                { title: 'P99(ms)', dataIndex: 'p99' },
              ]}
              scroll={{ x: 720 }}
            />
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card title="按协议调用分布">
            <Pie
              data={data.protocolCalls}
              angleField="value"
              colorField="protocol"
              height={280}
              innerRadius={0.6}
            />
            <ul aria-label="协议调用分布文本摘要">
              {data.protocolCalls.map((item) => (
                <li key={item.protocol}>{item.protocol}：{item.value}</li>
              ))}
            </ul>
          </Card>
        </Col>
      </Row>
    </section>
  )
}
