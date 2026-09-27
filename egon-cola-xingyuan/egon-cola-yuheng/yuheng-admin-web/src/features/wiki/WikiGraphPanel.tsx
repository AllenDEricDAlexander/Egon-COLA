import {useQuery} from '@tanstack/react-query'
import {Alert, Card, Empty, List, Space, Typography} from 'antd'
import {Link} from 'react-router-dom'
import {wikiApi} from '../../api/wiki'
import {LoadingBlock, QueryFailure} from '../../components/QueryState'

// 图节点/边只来自服务端授权后的已发布集合；这里不补全任何未授权标题。
export const WikiGraphPanel = ({ kbId, pageId }: { kbId: string; pageId?: string }) => {
  const graph = useQuery({
    queryKey: ['wiki', kbId, 'graph', {pageId}],
    queryFn: ({signal}) => wikiApi.graph(kbId, pageId, 100, signal),
    retry: false,
  })

  const data = graph.data
  if (graph.isLoading || (!graph.isError && !data)) return <LoadingBlock />
  if (graph.isError || !data) {
    return <QueryFailure error={graph.error} retry={() => void graph.refetch()} />
  }

  const titles = new Map(data.nodes.map((node) => [node.id, node.title]))
  return (
    <Card title="关联与来源" size="small">
      <Space direction="vertical" size="small" style={{width: '100%'}}>
        {data.truncated && (
          <Alert
            type="warning"
            showIcon
            title="已达到图输出上限"
            description="仅展示授权范围内的部分节点，请缩小范围后重试。"
          />
        )}
        {data.nodes.length === 0 ? (
          <Empty description="暂无已发布页面" />
        ) : (
          <List
            size="small"
            dataSource={data.nodes}
            renderItem={(node) => (
              <List.Item key={node.id}>
                <Link to={`/knowledge/${kbId}/wiki/${node.id}`}>{node.title}</Link>
              </List.Item>
            )}
          />
        )}
        <Typography.Text type="secondary">
          {`一跳链接 ${data.edges.length} 条`}
        </Typography.Text>
        <List
          size="small"
          dataSource={data.edges}
          locale={{emptyText: '无链接'}}
          renderItem={(edge) => (
            <List.Item key={`${edge.source}:${edge.target}`}>
              {`${titles.get(edge.source) ?? edge.source} → ${titles.get(edge.target) ?? edge.target}`}
            </List.Item>
          )}
        />
      </Space>
    </Card>
  )
}
