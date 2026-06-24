import { useState, useEffect } from 'react'
import {
  Button, Tag, Space, Modal, Form, Input, Select, message,
  Card, Row, Col, Statistic, Empty, Steps, Badge, Tooltip, Popconfirm
} from 'antd'
import {
  PlusOutlined, FileTextOutlined, VideoCameraOutlined,
  BookOutlined, CheckCircleOutlined, SyncOutlined,
  ClockCircleOutlined, ExclamationCircleOutlined, DeleteOutlined,
  EyeOutlined, PlayCircleOutlined, GiftOutlined
} from '@ant-design/icons'
import request from '@/api/request'
import { useNavigate } from 'react-router-dom'

interface Course {
  id: number
  title: string
  status: string
  knowledgePointId: number
  createdAt: string
  outline?: any
  script?: string
}

interface KnowledgePoint {
  id: number
  subject: string
  grade: string
  content: string
}

const PPT_TEMPLATES = [
  { value: 'default', label: '学院蓝', color: '#1F4E79', bg: 'bg-blue-600', desc: '经典专业风格，适合正式教学', icon: '🎓' },
  { value: 'elegant', label: '典雅绿', color: '#1A4731', bg: 'bg-emerald-700', desc: '沉稳典雅，适合学术讲解', icon: '📗' },
  { value: 'minimal', label: '简约白', color: '#2C3E50', bg: 'bg-slate-700', desc: '简洁现代，适合知识梳理', icon: '📋' },
  { value: 'vibrant', label: '活力橙', color: '#E67E22', bg: 'bg-orange-500', desc: '活泼鲜明，适合兴趣引导', icon: '🎨' },
]

const CoursePage = () => {
  const [data, setData] = useState<Course[]>([])
  const [knowledgePoints, setKnowledgePoints] = useState<KnowledgePoint[]>([])
  const [loading, setLoading] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [pptTemplateModal, setPptTemplateModal] = useState(false)
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)
  const [pptTemplate, setPptTemplate] = useState('default')
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const [pageNum] = useState(1)
  const [pageSize] = useState(999)
  const [total, setTotal] = useState(0)

  const statusConfig: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
    draft: { color: 'default', icon: <ClockCircleOutlined />, label: '草稿' },
    generating: { color: 'processing', icon: <SyncOutlined spin />, label: '生成中' },
    generated: { color: 'success', icon: <CheckCircleOutlined />, label: '已生成' },
    failed: { color: 'error', icon: <ExclamationCircleOutlined />, label: '失败' },
  }

  const loadKnowledgePoints = async () => {
    try {
      const res = await request.get('/knowledge-point/page?pageNum=1&pageSize=100') as any
      setKnowledgePoints(res?.records || [])
    } catch { message.error('加载知识点失败') }
  }

  const handleCreateCourse = async () => {
    try {
      const values = await form.validateFields()
      setGenerating(true)
      const res = await request.post('/course/generate', {
        knowledgePointId: values.knowledgePointId,
        title: values.title || undefined,
        chapterCount: values.chapterCount || undefined,
      }) as any
      if (res && res.code === 200) {
        message.success('课程生成成功')
        setCreateModalVisible(false)
        form.resetFields()
        loadData()
      } else {
        message.error(res?.message || '生成失败')
      }
    } catch (error: any) {
      if (!error.errorFields) message.error(error?.message || '生成失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleGeneratePpt = async () => {
    if (!selectedCourseId) return
    setGenerating(true)
    try {
      const res = await request.post('/material/ppt/generate', {
        courseId: selectedCourseId,
        template: pptTemplate,
      }) as any
      if (res && res.code === 200) {
        message.success('PPT生成成功')
        setPptTemplateModal(false)
        loadData()
      } else {
        message.error(res?.message || 'PPT生成失败')
      }
    } catch { message.error('PPT生成失败') }
    finally { setGenerating(false) }
  }

  const handleGenerateVideo = async (courseId: number) => {
    try {
      const res = await request.post('/material/video/generate', { courseId }) as any
      if (res && res.code === 200) {
        message.success('视频生成任务已创建')
        const taskId = res.data?.taskId
        if (taskId) checkVideoStatus(taskId)
      } else {
        message.error(res?.message || '视频生成失败')
      }
    } catch { message.error('视频生成失败') }
  }

  const checkVideoStatus = (taskId: number) => {
    const interval = setInterval(async () => {
      try {
        const res = await request.get(`/material/task/${taskId}`) as any
        const status = res?.status
        if (status === 'completed') { clearInterval(interval); message.success('视频生成完成'); loadData() }
        else if (status === 'failed') { clearInterval(interval); message.error('视频生成失败') }
      } catch { clearInterval(interval) }
    }, 3000)
  }

  const handleDeleteCourse = async (id: number) => {
    try {
      await request.delete(`/course/${id}`)
      message.success('删除成功')
      loadData()
    } catch { message.error('删除失败') }
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/course/page', { params: { pageNum, pageSize } }) as any
      setData(res?.records || [])
      setTotal(res?.total || 0)
    } catch { message.error('加载数据失败') }
    finally { setLoading(false) }
  }

  useEffect(() => { loadData() }, [])

  const generatedCount = data.filter(c => c.status === 'generated').length
  const generatingCount = data.filter(c => c.status === 'generating').length

  return (
    <div>
      {/* Page Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">课程管理</h2>
          <p className="text-gray-400 text-sm mt-1">从知识点出发，快速生成完整课程</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => { loadKnowledgePoints(); setCreateModalVisible(true) }}
          className="shadow-lg"
        >
          创建课程
        </Button>
      </div>

      {/* Stats */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-blue-50 to-blue-100 border-0 shadow-sm">
            <Statistic title={<span className="text-blue-600 font-medium">课程总数</span>} value={total}
              prefix={<BookOutlined className="text-blue-500" />} valueStyle={{ color: '#1890ff', fontSize: 28 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-green-50 to-green-100 border-0 shadow-sm">
            <Statistic title={<span className="text-green-600 font-medium">已生成</span>} value={generatedCount}
              prefix={<CheckCircleOutlined className="text-green-500" />} valueStyle={{ color: '#52c41a', fontSize: 28 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-orange-50 to-orange-100 border-0 shadow-sm">
            <Statistic title={<span className="text-orange-600 font-medium">生成中</span>} value={generatingCount}
              prefix={<SyncOutlined className="text-orange-500" />} valueStyle={{ color: '#fa8c16', fontSize: 28 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-purple-50 to-purple-100 border-0 shadow-sm">
            <Statistic title={<span className="text-purple-600 font-medium">利用率</span>}
              value={total > 0 ? Math.round(generatedCount / total * 100) : 0}
              suffix="%" prefix={<GiftOutlined className="text-purple-500" />} valueStyle={{ color: '#722ed1', fontSize: 28 }} />
          </Card>
        </Col>
      </Row>

      {/* Course Grid */}
      {data.length === 0 && !loading ? (
        <Card className="text-center py-16">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={
            <span className="text-gray-400">还没有课程，请先添加知识点再创建课程</span>
          }>
            <Button type="primary" onClick={() => navigate('/knowledge')}>去添加知识点</Button>
          </Empty>
        </Card>
      ) : (
        <Row gutter={[20, 20]}>
          {data.map(course => {
            const sc = statusConfig[course.status] || statusConfig.draft
            return (
              <Col span={8} key={course.id}>
                <Card
                  className="relative overflow-hidden hover:shadow-xl transition-all duration-300 cursor-pointer group"
                  styles={{ body: { padding: 0 } }}
                  onClick={() => navigate(`/course/${course.id}`)}
                  cover={
                    <div
                      className="h-32 relative flex items-center justify-center"
                      style={{
                        background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`,
                      }}
                    >
                      <div className="absolute inset-0 opacity-10"
                        style={{
                          backgroundImage: 'radial-gradient(circle at 20% 50%, white 0%, transparent 50%), radial-gradient(circle at 80% 50%, white 0%, transparent 50%)',
                        }}
                      />
                      <BookOutlined className="text-white text-5xl opacity-80" />
                      <div className="absolute top-3 right-3">
                        <Badge status={sc.color as any} text={<span className="text-white text-xs drop-shadow">{sc.label}</span>} />
                      </div>
                    </div>
                  }
                  actions={[
                    <Tooltip title="查看详情" key="view"><EyeOutlined key="view" onClick={(e) => { e.stopPropagation(); navigate(`/course/${course.id}`) }} /></Tooltip>,
                    <Tooltip title="生成PPT" key="ppt"><FileTextOutlined key="ppt" onClick={(e) => { e.stopPropagation(); setSelectedCourseId(course.id); setPptTemplateModal(true) }} /></Tooltip>,
                    <Tooltip title="生成视频" key="video"><VideoCameraOutlined key="video" onClick={(e) => { e.stopPropagation(); handleGenerateVideo(course.id) }} /></Tooltip>,
                    <Popconfirm title="确定删除？" key="delete" onConfirm={(e) => { e?.stopPropagation(); handleDeleteCourse(course.id) }}>
                      <DeleteOutlined key="delete" onClick={(e) => e.stopPropagation()} />
                    </Popconfirm>,
                  ]}
                >
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-800 text-base mb-1 group-hover:text-indigo-600 transition-colors line-clamp-2">
                      {course.title || '未命名课程'}
                    </h3>
                    <p className="text-gray-400 text-xs mb-3">#{course.id} · {course.createdAt?.substring(0, 10)}</p>
                    {course.status === 'generated' && (
                      <div className="flex gap-2">
                        <Tag icon={<FileTextOutlined />} color="orange" className="text-xs">PPT</Tag>
                        <Tag icon={<VideoCameraOutlined />} color="blue" className="text-xs">视频</Tag>
                      </div>
                    )}
                    {course.status === 'generating' && (
                      <Tag icon={<SyncOutlined spin />} color="processing" className="text-xs">生成中...</Tag>
                    )}
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      )}

      {/* Create Course Modal */}
      <Modal
        title={<span className="text-lg font-semibold">✨ 创建新课程</span>}
        open={createModalVisible}
        onOk={handleCreateCourse}
        onCancel={() => { setCreateModalVisible(false); form.resetFields() }}
        confirmLoading={generating}
        width={520}
        destroyOnClose
      >
        <div className="py-4">
          <Steps current={0} className="mb-6" items={[
            { title: '选择知识点', description: '课程核心内容' },
            { title: 'AI生成课程', description: '自动生成大纲' },
            { title: '生成内容', description: 'PPT和视频' },
          ]} />
          <Form form={form} layout="vertical">
            <Form.Item
              name="knowledgePointId"
              label="选择知识点"
              rules={[{ required: true, message: '请选择知识点' }]}
            >
              <Select
                size="large"
                placeholder="选择一个知识点作为课程核心"
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.children as any)?.props?.children?.toLowerCase?.().includes(input.toLowerCase()) ?? false
                }
              >
                {knowledgePoints.map(kp => (
                  <Select.Option key={kp.id} value={kp.id}>
                    <div className="flex items-center gap-2">
                      <Tag color="blue" className="text-xs">{kp.subject}</Tag>
                      <Tag color="purple" className="text-xs">{kp.grade}</Tag>
                      <span className="text-gray-600 text-sm truncate max-w-xs">{kp.content.substring(0, 40)}...</span>
                    </div>
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item name="title" label="课程标题（可选）">
              <Input placeholder="不填则由AI根据知识点自动生成" size="large" />
            </Form.Item>
            <Form.Item name="chapterCount" label="章节数量">
              <Select placeholder="默认4章" allowClear size="large">
                {[2, 3, 4, 5, 6, 7, 8].map(n => <Select.Option key={n} value={n}>{n} 章</Select.Option>)}
              </Select>
            </Form.Item>
          </Form>
        </div>
      </Modal>

      {/* PPT Template Modal */}
      <Modal
        title={<span className="text-lg font-semibold">🎨 选择PPT模板</span>}
        open={pptTemplateModal}
        onOk={handleGeneratePpt}
        confirmLoading={generating}
        onCancel={() => setPptTemplateModal(false)}
        width={560}
        destroyOnClose
        okText="开始生成"
      >
        <div className="py-4 space-y-3">
          <p className="text-gray-500 text-sm mb-4">选择配色风格，AI将生成对应风格的PPT课件：</p>
          {PPT_TEMPLATES.map(t => (
            <div
              key={t.value}
              onClick={() => setPptTemplate(t.value)}
              className={`cursor-pointer rounded-xl border-2 p-4 flex items-center gap-4 transition-all ${
                pptTemplate === t.value
                  ? 'border-indigo-500 bg-indigo-50 shadow-md'
                  : 'border-gray-100 bg-white hover:border-indigo-200 hover:shadow-sm'
              }`}
            >
              <div
                className={`w-14 h-14 rounded-xl ${t.bg} flex items-center justify-center text-2xl shadow-sm`}
              >
                {t.icon}
              </div>
              <div className="flex-1">
                <div className="font-semibold text-gray-800">{t.label}</div>
                <div className="text-sm text-gray-400 mt-0.5">{t.desc}</div>
              </div>
              {pptTemplate === t.value && (
                <CheckCircleOutlined className="text-indigo-500 text-xl" />
              )}
            </div>
          ))}
        </div>
      </Modal>
    </div>
  )
}

export default CoursePage
