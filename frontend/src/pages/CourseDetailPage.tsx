import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card, Button, Tag, Spin, message, Space, Modal,
  Breadcrumb, Descriptions, Empty, Tooltip, Divider, Collapse, Row, Col
} from 'antd'
import {
  ArrowLeftOutlined, FileTextOutlined, VideoCameraOutlined,
  CheckCircleOutlined, ClockCircleOutlined, SyncOutlined,
  CalendarOutlined, BookOutlined, ThunderboltOutlined, EditOutlined,
  EyeOutlined, DownloadOutlined, FolderOutlined
} from '@ant-design/icons'
import request from '@/api/request'
import { userStore } from '@/store/userStore'

interface Course {
  id: number
  title: string
  status: string
  knowledgePointId: number
  createdAt: string
  outline?: any
  script?: string
}

interface Material {
  id: number
  courseId: number
  knowledgePointId?: number
  materialType: string
  title: string
  fileUrl: string
  fileSize: number
  status: string
  createdAt: string
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

  // 素材
  const [materials, setMaterials] = useState<Material[]>([])
  const [materialsLoading, setMaterialsLoading] = useState(false)

  // 预览
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewType, setPreviewType] = useState<'video' | 'ppt'>('ppt')
  const [previewUrl, setPreviewUrl] = useState('')
  const [previewLoading, setPreviewLoading] = useState(false)
  const [pdfSrc, setPdfSrc] = useState('')

  useEffect(() => {
    if (!id) return
    loadCourse(+id)
    loadMaterials(+id)
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
        navigate('/app/courses')
      }
    } catch {
      message.error('加载课程详情失败')
      navigate('/app/courses')
    } finally {
      setLoading(false)
    }
  }

  const loadMaterials = async (courseId: number) => {
    setMaterialsLoading(true)
    try {
      const res = await request.get('/material/list', { params: { courseId } }) as any
      const list: Material[] = res?.data || res || []
      setMaterials(list.map((m: any) => ({
        ...m,
        knowledgePointId: m.knowledgePointId ?? m.knowledge_point_id ?? undefined,
      })))
    } catch { /* ignore */ }
    finally { setMaterialsLoading(false) }
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
      navigate('/app/tasks')
    } catch (e: any) {
      message.error(e?.response?.data?.message || '视频生成失败')
    } finally {
      setGeneratingVideo(false)
    }
  }

  const toProxyUrl = (fileUrl: string) => {
    if (!fileUrl) return ''
    return window.location.origin + fileUrl.replace('http://minio:9000/ai-teacher/', '/minio/ai-teacher/')
  }

  const handlePreview = (material: Material) => {
    const url = toProxyUrl(material.fileUrl)
    setPreviewUrl(url)
    setPreviewType(material.materialType === 'video' ? 'video' : 'ppt')
    setPreviewVisible(true)
  }

  // PPT预览 PDF加载
  useEffect(() => {
    if (!previewVisible || previewType !== 'ppt' || !previewUrl) return
    setPreviewLoading(true)
    setPdfSrc('')
    const objectName = previewUrl.replace(/.*\/minio\/ai-teacher\//, '')
    const apiUrl = `/api/ppt/preview?objectName=${encodeURIComponent(objectName)}`
    const token = userStore.getState().token
    if (!token) { setPreviewLoading(false); return }
    fetch(apiUrl, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.blob() })
      .then(blob => {
        setPdfSrc(URL.createObjectURL(blob))
        setPreviewLoading(false)
      })
      .catch(() => { message.error('PPT预览加载失败'); setPreviewLoading(false) })
    return () => { if (pdfSrc) URL.revokeObjectURL(pdfSrc) }
  }, [previewVisible, previewUrl, previewType])

  const formatSize = (size: number) => {
    if (!size) return '-'
    if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
    return (size / (1024 * 1024)).toFixed(1) + ' MB'
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

  const pptMaterials = materials.filter(m => m.materialType === 'ppt')
  const videoMaterials = materials.filter(m => m.materialType === 'video')

  const renderMaterialBadge = (m: Material) => {
    const isReady = m.status === 'generated' && m.fileUrl
    return (
      <Card
        key={m.id}
        size="small"
        className="hover:shadow-md transition-shadow"
        styles={{ body: { padding: 12 } }}
        actions={[
          <Tooltip title="预览" key="preview">
            {isReady
              ? <EyeOutlined onClick={() => handlePreview(m)} />
              : <span className="text-gray-300 cursor-not-allowed"><EyeOutlined /></span>
            }
          </Tooltip>,
          <Tooltip title="下载" key="download">
            {m.fileUrl
              ? <DownloadOutlined onClick={() => window.open(toProxyUrl(m.fileUrl), '_blank')} />
              : <span className="text-gray-300 cursor-not-allowed"><DownloadOutlined /></span>
            }
          </Tooltip>,
        ]}
      >
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
            style={{
              background: m.materialType === 'video'
                ? 'linear-gradient(135deg, #1e3a5f, #0f2744)'
                : 'linear-gradient(135deg, #fa8c16, #d97706)',
            }}
          >
            {m.materialType === 'video'
              ? <VideoCameraOutlined className="text-white text-lg" />
              : <FileTextOutlined className="text-white text-lg" />
            }
          </div>
          <div className="flex-1 min-w-0">
            <div className="font-medium text-gray-800 text-sm truncate">{m.title || '未命名'}</div>
            <div className="flex items-center gap-2 mt-0.5">
              <Tag color={m.materialType === 'video' ? 'blue' : 'orange'} className="text-xs">
                {m.materialType === 'video' ? '视频' : 'PPT'}
              </Tag>
              <span className="text-gray-400 text-xs">{formatSize(m.fileSize)}</span>
              <span className="text-gray-300 text-xs">·</span>
              <span className="text-gray-400 text-xs">{m.createdAt?.substring(0, 10)}</span>
            </div>
          </div>
          {m.status === 'generated' && m.fileUrl && (
            <Tag color="green" className="text-xs shrink-0">
              <CheckCircleOutlined /> 可用
            </Tag>
          )}
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-5">
      {/* Breadcrumb */}
      <Breadcrumb
        items={[
          { title: <a onClick={() => navigate('/app')}>首页</a> },
          { title: <a onClick={() => navigate('/app/courses')}>课程管理</a> },
          { title: course.title || '课程详情' },
        ]}
      />

      {/* Header Card */}
      <Card className="shadow-sm" styles={{ body: { padding: 0 } }}>
        <div
          className="h-36 relative flex items-center px-8"
          style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}
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

      {/* 课程素材 */}
      <Card
        title={
          <span className="text-lg font-semibold flex items-center gap-2">
            <FolderOutlined className="text-indigo-500" />
            课程素材
            {materials.length > 0 && <Tag color="purple" className="text-xs">{materials.length}个文件</Tag>}
          </span>
        }
        className="shadow-sm"
        extra={
          <Button
            type="primary"
            icon={<FileTextOutlined />}
            size="small"
            onClick={() => navigate('/app/materials')}
          >
            教材中心
          </Button>
        }
      >
        {materialsLoading ? (
          <div className="text-center py-8"><Spin size="small" /></div>
        ) : materials.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <FolderOutlined className="text-3xl mb-2 block" />
            <p className="text-sm">暂无课程素材，生成PPT或视频后将显示在这里</p>
          </div>
        ) : (
          <div className="space-y-3">
            {/* PPT素材 */}
            {pptMaterials.length > 0 && (
              <div>
                <div className="flex items-center gap-2 mb-2">
                  <FileTextOutlined className="text-orange-500" />
                  <span className="text-sm font-medium text-gray-600">PPT课件</span>
                  <Tag color="orange" className="text-xs">{pptMaterials.length}</Tag>
                </div>
                <div className="space-y-2">
                  {pptMaterials.map(m => renderMaterialBadge(m))}
                </div>
              </div>
            )}
            {/* 视频素材 */}
            {videoMaterials.length > 0 && (
              <div>
                {pptMaterials.length > 0 && <Divider className="my-3" />}
                <div className="flex items-center gap-2 mb-2">
                  <VideoCameraOutlined className="text-blue-500" />
                  <span className="text-sm font-medium text-gray-600">教学视频</span>
                  <Tag color="blue" className="text-xs">{videoMaterials.length}</Tag>
                </div>
                <div className="space-y-2">
                  {videoMaterials.map(m => renderMaterialBadge(m))}
                </div>
              </div>
            )}
          </div>
        )}
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
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/courses')} size="large">
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

      {/* Preview Modal */}
      <Modal
        title={
          <span className="flex items-center gap-2">
            {previewType === 'video'
              ? <VideoCameraOutlined style={{ color: '#1890ff' }} />
              : <FileTextOutlined style={{ color: '#fa8c16' }} />
            }
            {previewType === 'video' ? '视频预览' : 'PPT预览'}
          </span>
        }
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={
          <div className="flex justify-end gap-3">
            <Button icon={<DownloadOutlined />} onClick={() => window.open(previewUrl, '_blank')}>
              下载{previewType === 'video' ? '视频' : 'PPT'}
            </Button>
            <Button onClick={() => setPreviewVisible(false)}>关闭</Button>
          </div>
        }
        width={previewType === 'video' ? 680 : 900}
        destroyOnClose
      >
        <div style={{ marginTop: 16 }}>
          {previewType === 'video' ? (
            <video
              src={previewUrl}
              controls
              autoPlay
              style={{
                width: '100%', maxHeight: '60vh', borderRadius: 12,
                boxShadow: '0 4px 24px rgba(0,0,0,0.15)', background: '#000',
              }}
            />
          ) : (
            <div style={{ background: '#1e1e1e', borderRadius: 12, overflow: 'hidden', minHeight: 480, position: 'relative' }}>
              {previewLoading && (
                <div style={{
                  position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  background: '#1e1e1e', zIndex: 1, color: '#aaa', fontSize: 14, gap: 12,
                }}>
                  <span>正在转换 PPT，请稍候...</span>
                </div>
              )}
              <iframe
                key={pdfSrc || 'empty'}
                id="ppt-preview-iframe"
                title="PPT Preview"
                src={pdfSrc}
                style={{ width: '100%', height: 600, border: 'none', display: 'block' }}
              />
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}

export default CourseDetailPage
