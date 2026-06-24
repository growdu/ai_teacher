import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card, Button, Tag, Spin, message, Space, Modal,
  Breadcrumb, Descriptions, Empty, Tooltip, Divider, Collapse
} from 'antd'
import {
  ArrowLeftOutlined, FileTextOutlined, VideoCameraOutlined,
  CheckCircleOutlined, ClockCircleOutlined, SyncOutlined,
  CalendarOutlined, BookOutlined, ThunderboltOutlined, EditOutlined
} from '@ant-design/icons'
import request from '@/api/request'

interface Course {
  id: number
  title: string
  status: string
  knowledgePointId: number
  createdAt: string
  outline?: any
  script?: string
}

interface Chapter {
  title: string
  duration?: number
  keyPoints: string[]
  teachingNotes?: string
}

interface Outline {
  title: string
  description?: string
  chapters: Chapter[]
  totalDuration?: number
}

const CourseDetailPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [course, setCourse] = useState<Course | null>(null)
  const [loading, setLoading] = useState(true)
  const [outlineData, setOutlineData] = useState<Outline | null>(null)
  const [videoModalVisible, setVideoModalVisible] = useState(false)
  const [videoScript, setVideoScript] = useState('')
  const [generatingVideo, setGeneratingVideo] = useState(false)

  useEffect(() => {
    if (!id) return
    loadCourse(+id)
  }, [id])

  const loadCourse = async (courseId: number) => {
    setLoading(true)
    try {
      const res = await request.get(`/course/${courseId}`) as any
      const courseData = res && res.code === 200 ? res.data : (res || null)
      if (courseData) {
        setCourse(courseData)
        if (courseData.outline) {
          try {
            const parsed = typeof courseData.outline === 'string'
              ? JSON.parse(courseData.outline)
              : courseData.outline
            setOutlineData(parsed)
          } catch {
            setOutlineData(null)
          }
        }
      } else {
        message.error(res?.message || '加载失败')
        navigate('/courses')
      }
    } catch {
      message.error('加载课程详情失败')
      navigate('/courses')
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateVideo = async () => {
    if (!course) return
    setGeneratingVideo(true)
    try {
      await request.post('/material/video/generate', {
        courseId: course.id,
        script: videoScript || course.script,
      })
      message.success('视频生成任务已创建')
      setVideoModalVisible(false)
      navigate('/tasks')
    } catch (e: any) {
      message.error(e?.response?.data?.message || '视频生成失败')
    } finally {
      setGeneratingVideo(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Spin tip="加载课程内容..." size="large" />
      </div>
    )
  }

  if (!course) return null

  const statusConfig: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
    draft: { color: 'default', icon: <ClockCircleOutlined />, label: '草稿' },
    generating: { color: 'processing', icon: <SyncOutlined spin />, label: '生成中' },
    generated: { color: 'success', icon: <CheckCircleOutlined />, label: '已生成' },
    failed: { color: 'error', icon: <ClockCircleOutlined />, label: '失败' },
  }
  const sc = statusConfig[course.status] || statusConfig.draft

  return (
    <div className="space-y-5">
      {/* Breadcrumb */}
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate('/')}>首页</a> },
          { title: <a onClick={() => navigate('/courses')}>课程管理</a> },
          { title: course.title || '课程详情' },
        ]}
      />

      {/* Header Card */}
      <Card
        className="shadow-sm"
        styles={{ body: { padding: 0 } }}
      >
        <div
          className="h-36 relative flex items-center px-8"
          style={{
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          }}
        >
          <div className="absolute inset-0 opacity-10"
            style={{
              backgroundImage: 'radial-gradient(circle at 30% 70%, white 0%, transparent 40%), radial-gradient(circle at 70% 30%, white 0%, transparent 40%)',
            }}
          />
          <div className="relative z-10 flex items-center gap-5 w-full">
            <div className="w-20 h-20 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center shadow-lg">
              <BookOutlined className="text-white text-3xl" />
            </div>
            <div className="flex-1">
              <h1 className="text-white text-2xl font-bold mb-1 drop-shadow">
                {course.title || '未命名课程'}
              </h1>
              <Space className="mt-2">
                <Tag icon={sc.icon} color={sc.color} className="bg-white/20 border-0 text-white">
                  {sc.label}
                </Tag>
                <span className="text-white/70 text-sm flex items-center gap-1">
                  <CalendarOutlined /> {course.createdAt?.substring(0, 10)}
                </span>
              </Space>
            </div>
            <Space className="relative z-10">
              <Button
                icon={<FileTextOutlined />}
                onClick={() => navigate(`/quiz?courseId=${course.id}`)}
                className="shadow-lg border-0"
                style={{ background: 'rgba(255,255,255,0.2)', color: 'white' }}
              >
                生成测验
              </Button>
              <Button
                icon={<VideoCameraOutlined />}
                onClick={() => { setVideoScript(course?.script || ''); setVideoModalVisible(true) }}
                className="shadow-lg border-0"
                style={{ background: 'rgba(255,255,255,0.2)', color: 'white' }}
              >
                生成视频
              </Button>
            </Space>
          </div>
        </div>

        <div className="p-6">
          <Descriptions column={3} size="small">
            <Descriptions.Item label={<span className="text-gray-400"><BookOutlined className="mr-1" />课程ID</span>}>
              <span className="text-gray-500">#{course.id}</span>
            </Descriptions.Item>
            <Descriptions.Item label={<span className="text-gray-400">知识点ID</span>}>
              <span className="text-gray-500">#{course.knowledgePointId}</span>
            </Descriptions.Item>
            <Descriptions.Item label={<span className="text-gray-400">状态</span>}>
              <Tag icon={sc.icon} color={sc.color}>{sc.label}</Tag>
            </Descriptions.Item>
          </Descriptions>
        </div>
      </Card>

      {/* Course Outline */}
      {outlineData ? (
        <Card title={
          <span className="text-lg font-semibold flex items-center gap-2">
            <ThunderboltOutlined className="text-indigo-500" />
            课程大纲
            {outlineData.totalDuration && (
              <Tag className="ml-2 text-xs">{outlineData.totalDuration} 分钟</Tag>
            )}
          </span>
        } className="shadow-sm">
          {outlineData.description && (
            <div className="bg-gradient-to-r from-indigo-50 to-purple-50 rounded-xl p-4 mb-5 border border-indigo-100">
              <p className="text-gray-600 leading-relaxed">{outlineData.description}</p>
            </div>
          )}

          <Collapse
            bordered={false}
            className="bg-transparent"
            expandIconPosition="start"
            items={outlineData.chapters?.map((ch: Chapter, idx: number) => ({
              key: String(idx),
              label: (
                <div className="flex items-center justify-between w-full pr-4">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-indigo-500 text-white flex items-center justify-center font-bold text-sm shadow-sm">
                      {idx + 1}
                    </div>
                    <div>
                      <span className="font-semibold text-gray-800">{ch.title}</span>
                      {ch.duration && (
                        <span className="text-gray-400 text-xs ml-2">{ch.duration}分钟</span>
                      )}
                    </div>
                  </div>
                  {ch.keyPoints && ch.keyPoints.length > 0 && (
                    <Tag color="blue" className="text-xs">{ch.keyPoints.length}个重点</Tag>
                  )}
                </div>
              ),
              children: (
                <div className="space-y-3 pl-1">
                  {ch.keyPoints && ch.keyPoints.length > 0 && (
                    <div>
                      <h5 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1">
                        <span className="w-1 h-1 rounded-full bg-indigo-400 inline-block" />
                        核心要点
                      </h5>
                      <div className="space-y-1.5">
                        {ch.keyPoints.map((kp, i) => (
                          <div key={i} className="flex items-start gap-2 text-sm text-gray-600">
                            <CheckCircleOutlined className="text-green-500 text-xs mt-1 shrink-0" />
                            <span>{kp}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  {ch.teachingNotes && (
                    <div>
                      <h5 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1">
                        <span className="w-1 h-1 rounded-full bg-orange-400 inline-block" />
                        教学备注
                      </h5>
                      <div className="bg-orange-50 rounded-lg p-3 text-sm text-orange-800 leading-relaxed">
                        {ch.teachingNotes}
                      </div>
                    </div>
                  )}
                </div>
              ),
            })) || []}
          />
        </Card>
      ) : (
        <Card className="shadow-sm text-center py-12">
          <Empty description={<span className="text-gray-400">暂无课程大纲</span>} />
        </Card>
      )}

      {/* Script */}
      {course.script && (
        <Card
          title={
            <span className="text-lg font-semibold flex items-center gap-2">
              <EditOutlined className="text-indigo-500" />
              讲稿脚本
            </span>
          }
          className="shadow-sm"
        >
          <div
            className="bg-gray-50 rounded-xl p-5 text-sm text-gray-700 leading-loose whitespace-pre-wrap border border-gray-100 max-h-80 overflow-y-auto"
          >
            {course.script}
          </div>
        </Card>
      )}

      {/* Back button */}
      <div className="flex justify-start">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/courses')} size="large">
          返回课程列表
        </Button>
      </div>

      {/* Video Generation Modal */}
      <Modal
        title={<span className="text-lg font-semibold flex items-center gap-2">
          <VideoCameraOutlined className="text-blue-500" />生成教学视频
        </span>}
        open={videoModalVisible}
        onCancel={() => setVideoModalVisible(false)}
        footer={null}
        destroyOnClose
        width={600}
      >
        <div className="py-4 space-y-4">
          <div className="bg-blue-50 rounded-lg p-4 text-sm text-blue-700 border border-blue-100">
            使用课程讲稿脚本生成教学视频。如需自定义脚本内容，可在下方文本框中编辑。
          </div>
          <div>
            <label className="block font-medium text-gray-700 mb-2">讲稿脚本</label>
            <textarea
              rows={8}
              className="w-full border border-gray-200 rounded-xl p-3 text-sm leading-relaxed focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100 transition-all"
              value={videoScript}
              onChange={e => setVideoScript(e.target.value)}
              placeholder="输入或编辑视频讲稿脚本..."
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button onClick={() => setVideoModalVisible(false)}>取消</Button>
            <Button
              type="primary"
              icon={<VideoCameraOutlined />}
              loading={generatingVideo}
              onClick={handleGenerateVideo}
              className="shadow-lg"
            >
              开始生成视频
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default CourseDetailPage
