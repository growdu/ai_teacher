import { Card, Row, Col, Statistic, Table, Tag } from 'antd'
import { UserOutlined, BookOutlined, TeamOutlined, RiseOutlined } from '@ant-design/icons'

const Dashboard = () => {
  const columns = [
    {
      title: '学生姓名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '学习课程',
      dataIndex: 'course',
      key: 'course',
    },
    {
      title: '学习进度',
      dataIndex: 'progress',
      key: 'progress',
      render: (progress: number) => (
        <Tag color={progress >= 80 ? 'green' : progress >= 50 ? 'blue' : 'orange'}>
          {progress}%
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '学习中' ? 'processing' : 'default'}>
          {status}
        </Tag>
      ),
    },
  ]

  const data = [
    { key: '1', name: '张三', course: 'Python基础', progress: 85, status: '学习中' },
    { key: '2', name: '李四', course: '机器学习', progress: 60, status: '学习中' },
    { key: '3', name: '王五', course: '深度学习', progress: 30, status: '已暂停' },
    { key: '4', name: '赵六', course: 'Python基础', progress: 92, status: '已完成' },
  ]

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">仪表盘</h1>
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card>
            <Statistic
              title="学生总数"
              value={1258}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="课程总数"
              value={42}
              prefix={<BookOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="活跃学习"
              value={856}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="学习增长率"
              value={12.5}
              prefix={<RiseOutlined />}
              suffix="%"
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>
      <Card title="最近学习情况">
        <Table
          columns={columns}
          dataSource={data}
          pagination={false}
        />
      </Card>
    </div>
  )
}

export default Dashboard