import { useState, useEffect } from 'react'
import {
  Card, Row, Col, Statistic, Table, Tag, Button, Space,
  Progress, List, Avatar,
} from 'antd'
import {
  BookOutlined, ExperimentOutlined, FolderOutlined,
  PlayCircleOutlined, FileTextOutlined, ArrowRightOutlined,
  PlusOutlined, ThunderboltOutlined, CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import request from '@/api/request'
import { useNavigate } from 'react-router-dom'

interface DashboardStats {
  courseCount: number
  knowledgeCount: number
  materialCount: number
  activeTaskCount: number
  generatedCourseCount: number
  generationRate: number
  pptCount: number
  videoCount: number
}

interface Course {
  id: number
  title: string
  status: string
  createdAt: string
}

interface Material {
  id: number
  title: string
  materialType: string
  status: string
}

const Dashboard = () => {
  const navigate = useNavigate()
  const [stats, setStats] = useState<DashboardStats>({
    courseCount: 0, knowledgeCount: 0, materialCount: 0, activeTaskCount: 0,
    generatedCourseCount: 0, generationRate: 0, pptCount: 0, videoCount: 0,
  })
  const [recentCourses, setRecentCourses] = useState<Course[]>([])
  const [recentMaterials, setRecentMaterials] = useState<Material[]>([])

  useEffect(() => { loadDashboardData() }, [])

  const loadDashboardData = async () => {
    try {
      const [statsRes, courseRes, materialRes] = await Promise.allSettled([
        request.get('/dashboard/stats') as any,
        request.get('/course/page?pageNum=1&pageSize=5') as any,
        request.get('/material/page?pageNum=1&pageSize=5') as any,
      ])

      if (statsRes.status === 'fulfilled' && statsRes.value) {
        console.log('[DEBUG Dashboard] statsRes.value =', JSON.stringify(statsRes.value))
        setStats(statsRes.value as DashboardStats)
        console.log('[DEBUG Dashboard] stats state after setStats')
      } else {
        console.log('[DEBUG Dashboard] statsRes status =', statsRes.status, statsRes)
      }
      if (courseRes.status === 'fulfilled' && courseRes.value) {
        setRecentCourses(courseRes.value?.records || [])
      }
      if (materialRes.status === 'fulfilled' && materialRes.value) {
        setRecentMaterials(materialRes.value?.records || [])
      }
    } catch (error) {
      console.error('Failed to load dashboard data:', error)
    }
  }

  const statusConfig: Record<string, { color: string; icon: React.ReactNode }> = {
    draft: { color: 'default', icon: <ClockCircleOutlined /> },
    generating: { color: 'processing', icon: <ClockCircleOutlined /> },
    generated: { color: 'success', icon: <CheckCircleOutlined /> },
    failed: { color: 'error', icon: <ClockCircleOutlined /> },
  }

  const courseColumns = [
    {
      title: '课程',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: Course) => (
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-400 to-purple-500 flex items-center justify-center">
            <BookOutlined className="text-white text-xs" />
          </div>
          <span className="font-medium text-gray-700">{title || '未命名课程'}</span>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const sc = statusConfig[status] || statusConfig.draft
        return <Tag icon={sc.icon} color={sc.color}>{status === 'generated' ? '已生成' : status === 'draft' ? '草稿' : status === 'generating' ? '生成中' : status}</Tag>
      },
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (t: string) => <span className="text-gray-400 text-xs">{t?.substring(0, 10)}</span>,
    },
    {
      title: '',
      key: 'action',
      width: 80,
      render: (_: any, record: Course) => (
        <Button type="link" size="small" onClick={() => navigate(`/app/course/${record.id}`)}>
          查看 <ArrowRightOutlined />
        </Button>
      ),
    },
  ]

  const materialColumns = [
    {
      title: '教材',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: Material) => (
        <div className="flex items-center gap-2">
          {record.materialType === 'video'
            ? <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center"><PlayCircleOutlined className="text-white text-xs" /></div>
            : <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-orange-400 to-orange-600 flex items-center justify-center"><FileTextOutlined className="text-white text-xs" /></div>
          }
          <span className="font-medium text-gray-700">{title || '未命名教材'}</span>
        </div>
      ),
    },
    {
      title: '类型',
      dataIndex: 'materialType',
      key: 'materialType',
      width: 80,
      render: (t: string) => <Tag color={t === 'video' ? 'blue' : 'orange'}>{t === 'video' ? '视频' : 'PPT'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: string) => <Tag color={s === 'generated' ? 'success' : 'processing'}>{s === 'generated' ? '就绪' : '生成中'}</Tag>,
    },
    {
      title: '',
      key: 'action',
      width: 80,
      render: (_: any, record: Material) => (
        <Button type="link" size="small" onClick={() => navigate('/app/materials')}>
          查看 <ArrowRightOutlined />
        </Button>
      ),
    },
  ]

  const quickActions = [
    {
      title: '添加知识点',
      desc: '创建新的知识点内容',
      icon: <ExperimentOutlined />,
      color: '#52c41a',
      bg: 'from-green-50 to-green-100',
      path: '/app/knowledge',
    },
    {
      title: '创建课程',
      desc: '从知识点生成课程',
      icon: <BookOutlined />,
      color: '#1890ff',
      bg: 'from-blue-50 to-blue-100',
      path: '/app/courses',
    },
    {
      title: '生成PPT',
      desc: '制作教学课件',
      icon: <FileTextOutlined />,
      color: '#fa8c16',
      bg: 'from-orange-50 to-orange-100',
      path: '/app/materials',
    },
    {
      title: '生成视频',
      desc: '制作教学视频',
      icon: <PlayCircleOutlined />,
      color: '#722ed1',
      bg: 'from-purple-50 to-purple-100',
      path: '/app/courses',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-800">欢迎回来 👋</h1>
        <p className="text-gray-400 text-sm mt-1">这是您的教学数据概览</p>
      </div>

      {/* Stats Cards */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={8}>
          <Card className="border-0 shadow-sm hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate('/app/courses')}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm mb-1">课程总数</p>
                <span className="text-3xl font-bold" style={{ color: '#1890ff' }}>{stats.courseCount || '-'}</span>
              </div>
              <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center">
                <BookOutlined className="text-blue-500 text-xl" />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8}>
          <Card className="border-0 shadow-sm hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate('/app/knowledge')}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm mb-1">知识点</p>
                <span className="text-3xl font-bold" style={{ color: '#52c41a' }}>{stats.knowledgeCount || '-'}</span>
              </div>
              <div className="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center">
                <ExperimentOutlined className="text-green-500 text-xl" />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8}>
          <Card className="border-0 shadow-sm hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate('/app/materials')}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm mb-1">教材总数</p>
                <span className="text-3xl font-bold" style={{ color: '#722ed1' }}>{stats.materialCount || '-'}</span>
              </div>
              <div className="w-12 h-12 rounded-xl bg-purple-100 flex items-center justify-center">
                <FolderOutlined className="text-purple-500 text-xl" />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8}>
          <Card className="border-0 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm mb-1">课程生成率</p>
                <span className="text-3xl font-bold" style={{ color: '#fa8c16' }}>
                  {stats.generationRate || 0}%
                </span>
              </div>
              <div className="w-12 h-12 rounded-xl bg-orange-100 flex items-center justify-center">
                <ThunderboltOutlined className="text-orange-500 text-xl" />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Detailed Stats Row */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card className="border-0 shadow-sm" size="small">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-xs mb-1">已生成课程</p>
                <span className="text-lg font-bold text-green-600">{stats.generatedCourseCount || 0}</span>
              </div>
              <CheckCircleOutlined className="text-green-500 text-lg" />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="border-0 shadow-sm" size="small">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-xs mb-1">进行中任务</p>
                <span className="text-lg font-bold text-blue-600">{stats.activeTaskCount || 0}</span>
              </div>
              <ClockCircleOutlined className="text-blue-500 text-lg" />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="border-0 shadow-sm" size="small">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-xs mb-1">PPT教材</p>
                <span className="text-lg font-bold text-orange-600">{stats.pptCount || 0}</span>
              </div>
              <FileTextOutlined className="text-orange-500 text-lg" />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card className="border-0 shadow-sm" size="small">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-xs mb-1">视频教材</p>
                <span className="text-lg font-bold text-purple-600">{stats.videoCount || 0}</span>
              </div>
              <PlayCircleOutlined className="text-purple-500 text-lg" />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Quick Actions */}
      <Card title={<span className="font-semibold">⚡ 快捷操作</span>} className="shadow-sm">
        <Row gutter={[16, 16]}>
          {quickActions.map((action, idx) => (
            <Col xs={12} sm={12} md={6} key={idx}>
              <div
                onClick={() => navigate(action.path)}
                className={`cursor-pointer rounded-xl border-0 bg-gradient-to-br ${action.bg} p-4 hover:shadow-md transition-all hover:-translate-y-0.5`}
              >
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center mb-3"
                  style={{ background: action.color + '15' }}
                >
                  <span style={{ color: action.color, fontSize: 20 }}>{action.icon}</span>
                </div>
                <div className="font-semibold text-gray-800 text-sm mb-1">{action.title}</div>
                <div className="text-gray-400 text-xs hidden sm:block">{action.desc}</div>
              </div>
            </Col>
          ))}
        </Row>
      </Card>

      {/* Recent Lists */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title={<span className="font-semibold">📚 最近课程</span>}
            extra={<Button type="link" onClick={() => navigate('/app/courses')}>查看全部 <ArrowRightOutlined /></Button>}
            className="shadow-sm"
            styles={{ body: { padding: 0 } }}
          >
            <div className="overflow-x-auto">
              <Table
                columns={courseColumns}
                dataSource={recentCourses}
                rowKey="id"
                pagination={false}
                size="small"
                scroll={{ x: 'max-content' }}
              />
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={<span className="font-semibold">📦 最近教材</span>}
            extra={<Button type="link" onClick={() => navigate('/app/materials')}>查看全部 <ArrowRightOutlined /></Button>}
            className="shadow-sm"
            styles={{ body: { padding: 0 } }}
          >
            <div className="overflow-x-auto">
              <Table
                columns={materialColumns}
                dataSource={recentMaterials}
                rowKey="id"
                pagination={false}
                size="small"
                scroll={{ x: 'max-content' }}
              />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
