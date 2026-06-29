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
  const [pptForm] = Form.useForm()
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
      const values = await pptForm.validateFields()
      const res = await request.post('/material/ppt/generate', {
        courseId: selectedCourseId,
        template: pptTemplate,
        keywords: values.keywords || undefined,
        goals: values.goals || undefined,
        targetAudience: values.targetAudience || undefined,
        additionalNotes: values.additionalNotes || undefined,
      }) as any
      if (res && res.code === 200) {
        const data = res.data || {}
        const totalSlides = data.totalSlides
        const totalDuration = data.totalDuration
        const chapters = data.chapters || []
        const slideCountByType: Record<string, number> = {}
        chapters.forEach((ch: any) => {
          ;(ch.slides || []).forEach((s: any) => {
            slideCountByType[s.type] = (slideCountByType[s.type] || 0) + 1
          })
        })
        const typeStats = Object.entries(slideCountByType)
          .map(([t, c]) => `${t}×${c}`)
          .join(' | ')

        message.success({
          content: (
            <span>
              PPT生成成功
              {totalSlides && <span className="ml-3 text-sm opacity-80">📊 {totalSlides}页 | ⏱ {totalDuration}分钟{chapters.length ? ` | 📑 ${chapters.length}章节` : ''}</span>}
              {typeStats && <div className="text-xs mt-0.5 opacity-70">{typeStats}</div>}
            </span>
          ),
        })
        setPptTemplateModal(false)
        pptForm.resetFields()
        loadData()
      } else {
        message.error(res?.message || 'PPT生成失败')
      }
    } catch (error: any) {
      if (!error.errorFields) message.error(error?.message || 'PPT生成失败')
    }
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
            <Button type="primary" onClick={() => navigate('/app/knowledge')}>去添加知识点</Button>
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
        title={<span className="text-lg font-semibold">🎨 生成PPT课件</span>}
        open={pptTemplateModal}
        onOk={handleGeneratePpt}
        confirmLoading={generating}
        onCancel={() => { setPptTemplateModal(false); pptForm.resetFields() }}
        width={600}
        destroyOnClose
        okText="开始生成"
      >
        <div className="py-4 space-y-4">
          {/* 模板选择 */}
          <div>
            <p className="text-gray-500 text-sm mb-3 font-medium">配色风格</p>
            <div className="grid grid-cols-2 gap-3">
              {PPT_TEMPLATES.map(t => (
                <div
                  key={t.value}
                  onClick={() => setPptTemplate(t.value)}
                  className={`cursor-pointer rounded-xl border-2 p-3 flex items-center gap-3 transition-all ${
                    pptTemplate === t.value
                      ? 'border-indigo-500 bg-indigo-50 shadow-md'
                      : 'border-gray-100 bg-white hover:border-indigo-200 hover:shadow-sm'
                  }`}
                >
                  <div
                    className={`w-10 h-10 rounded-lg ${t.bg} flex items-center justify-center text-xl shadow-sm flex-shrink-0`}
                  >
                    {t.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-semibold text-gray-800 text-sm">{t.label}</div>
                    <div className="text-xs text-gray-400 mt-0.5 line-clamp-1">{t.desc}</div>
                  </div>
                  {pptTemplate === t.value && (
                    <CheckCircleOutlined className="text-indigo-500 text-lg flex-shrink-0" />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* 用户输入区域 */}
          <Form form={pptForm} layout="vertical" size="small">
            <div className="bg-gray-50 rounded-xl p-4 space-y-3">
              <p className="text-gray-500 text-sm mb-3 font-medium">📝 自定义内容要求（可选）</p>
              <Form.Item name="keywords" label="关键字">
                <Input placeholder="例如：三角函数、图像变换、历年真题" />
              </Form.Item>
              <Form.Item name="goals" label="学习目标">
                <Select allowClear placeholder="选择本节课的核心目标">
                  <Select.Option value="概念理解">概念理解</Select.Option>
                  <Select.Option value="解题技巧">解题技巧</Select.Option>
                  <Select.Option value="考试复习">考试复习</Select.Option>
                  <Select.Option value="兴趣引导">兴趣引导</Select.Option>
                  <Select.Option value="综合应用">综合应用</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item name="targetAudience" label="目标受众">
                <Select allowClear placeholder="AI将据此调整内容深浅">
                  <Select.Option value="小学生">小学生</Select.Option>
                  <Select.Option value="初中生">初中生</Select.Option>
                  <Select.Option value="高中生">高中生</Select.Option>
                  <Select.Option value="大学生">大学生</Select.Option>
                  <Select.Option value="成人">成人</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item name="additionalNotes" label="补充说明">
                <Input.TextArea placeholder="例如：需要联系生活实际、加强互动设计、侧重公式推导..." rows={2} />
              </Form.Item>
            </div>
          </Form>
        </div>
      </Modal>
    </div>
  )
}

export default CoursePage
