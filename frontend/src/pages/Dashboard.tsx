import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, List, Avatar, Button, Space } from 'antd'
import { UserOutlined, BookOutlined, FolderOutlined, RiseOutlined, PlayCircleOutlined, FileTextOutlined } from '@ant-design/icons'
import request from '@/api/request'
import { useNavigate } from 'react-router-dom'

const Dashboard = () => {
  const navigate = useNavigate()
  const [stats, setStats] = useState({
    courseCount: 0,
    knowledgeCount: 0,
    materialCount: 0,
    activeTaskCount: 0,
  })
  const [recentCourses, setRecentCourses] = useState<any[]>([])
  const [recentMaterials, setRecentMaterials] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    setLoading(true)
    try {
      // Load courses
      const courseRes = await request.get('/course/page?pageNum=1&pageSize=5')
      const courses = courseRes.data?.records || []
      setRecentCourses(courses)
      setStats(prev => ({ ...prev, courseCount: courseRes.data?.total || 0 }))

      // Load knowledge points
      const knowledgeRes = await request.get('/knowledge-point/page?pageNum=1&pageSize=1')
      setStats(prev => ({ ...prev, knowledgeCount: knowledgeRes.data?.total || 0 }))

      // Load materials
      const materialRes = await request.get('/material/page?pageNum=1&pageSize=5')
      const materials = materialRes.data?.records || []
      setRecentMaterials(materials)
      setStats(prev => ({ ...prev, materialCount: materialRes.data?.total || 0 }))

    } catch (error) {
      console.error('Failed to load dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const courseColumns = [
    {
      title: '课程标题',
      dataIndex: 'title',
      key: 'title',
      render: (title: string) => <a onClick={() => navigate('/courses')}>{title || '未命名课程'}</a>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const config: Record<string, { color: string; label: string }> = {
          draft: { color: 'default', label: '草稿' },
          generating: { color: 'processing', label: '生成中' },
          generated: { color: 'success', label: '已生成' },
          failed: { color: 'error', label: '失败' },
        }
        const c = config[status] || { color: 'default', label: status }
        return <Tag color={c.color}>{c.label}</Tag>
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
  ]

  const materialColumns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: any) => (
        <Space>
          {record.materialType === 'ppt' ? <FileTextOutlined style={{ color: 'orange' }} /> : <PlayCircleOutlined style={{ color: 'blue' }} />}
          <span>{title || '未命名教材'}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'materialType',
      key: 'materialType',
      render: (type: string) => <Tag color={type === 'ppt' ? 'orange' : 'blue'}>{type?.toUpperCase()}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'generated' ? 'success' : 'processing'}>{status}</Tag>
      ),
    },
  ]

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">仪表盘</h1>

      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card loading={loading}>
            <Statistic
              title="课程总数"
              value={stats.courseCount}
              prefix={<BookOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={loading}>
            <Statistic
              title="知识点总数"
              value={stats.knowledgeCount}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={loading}>
            <div onClick={() => navigate('/materials')} className="cursor-pointer">
              <Statistic
                title="教材总数"
                value={stats.materialCount}
                prefix={<FolderOutlined />}
                valueStyle={{ color: '#722ed1' }}
              />
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={loading}>
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

      <Row gutter={16}>
        <Col span={12}>
          <Card
            title="最近课程"
            extra={<Button type="link" onClick={() => navigate('/courses')}>查看更多</Button>}
          >
            <Table
              columns={courseColumns}
              dataSource={recentCourses}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card
            title="最近教材"
            extra={<Button type="link" onClick={() => navigate('/materials')}>查看更多</Button>}
          >
            <Table
              columns={materialColumns}
              dataSource={recentMaterials}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} className="mt-6">
        <Col span={24}>
          <Card title="快速开始">
            <Space size="large">
              <Button type="primary" size="large" onClick={() => navigate('/knowledge')}>
                添加知识点
              </Button>
              <Button size="large" onClick={() => navigate('/courses')}>
                创建课程
              </Button>
              <Button size="large" onClick={() => navigate('/settings')}>
                配置AI
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard