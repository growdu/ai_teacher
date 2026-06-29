import { useState, useEffect, useRef } from 'react'
import {
  Button, Tag, Space, message, Card, Row, Col, Statistic,
  Modal, Select, Empty, Tooltip, Badge, Popconfirm, Form, Input,
} from 'antd'
import {
  DownloadOutlined, DeleteOutlined, PlusOutlined,
  FileOutlined, VideoCameraOutlined, EyeOutlined,
  FileTextOutlined, PictureOutlined, CheckCircleOutlined,
  FolderOutlined
} from '@ant-design/icons'
import request from '@/api/request'
import { userStore } from '@/store/userStore'

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

interface CourseOption {
  id: number
  title: string
  status: string
}

interface KnowledgePointOption {
  id: number
  subject: string
  grade: string
  content: string
}

interface GroupedMaterials {
  knowledgePointId?: number
  knowledgePointName: string
  materials: Material[]
}

// 扩展接口含knowledgePointId
interface MaterialExt extends Material {
  knowledgePointId?: number
}

const PPT_TEMPLATES = [
  { value: 'default', label: '默认风格' },
  { value: 'elegant', label: '典雅风格' },
  { value: 'minimal', label: '简约风格' },
  { value: 'vibrant', label: '活力风格' },
]

const AI_MODELS = [
  { value: '', label: '自动选择（默认）' },
  { value: 'MiniMax', label: 'MiniMax' },
  { value: 'Claude', label: 'Claude' },
  { value: 'OpenAI', label: 'OpenAI' },
  { value: 'Qwen', label: 'Qwen' },
  { value: 'DeepSeek', label: 'DeepSeek' },
]

const MaterialPage = () => {
  const [data, setData] = useState<Material[]>([])
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [pptModalVisible, setPptModalVisible] = useState(false)
  const [courseList, setCourseList] = useState<CourseOption[]>([])
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)
  const [pptTemplate, setPptTemplate] = useState<string>('default')
  const [selectedModel, setSelectedModel] = useState<string>('')
  const [pptForm] = Form.useForm()

  // 知识点筛选
  const [kpList, setKpList] = useState<KnowledgePointOption[]>([])
  const [selectedKpId, setSelectedKpId] = useState<number | null>(null)

  const iframeRef = useRef<HTMLIFrameElement>(null)

  // 预览
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewType, setPreviewType] = useState<'video' | 'ppt'>('ppt')
  const [previewUrl, setPreviewUrl] = useState('')
  const [previewLoading, setPreviewLoading] = useState(false)
  const [pdfSrc, setPdfSrc] = useState('')

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

  const [pageNum] = useState(1)
  const [pageSize] = useState(999)

  const toProxyUrl = (fileUrl: string) => {
    if (!fileUrl) return ''
    return window.location.origin + fileUrl.replace('http://minio:9000/ai-teacher/', '/minio/ai-teacher/')
  }

  const handlePreview = (record: Material) => {
    const url = toProxyUrl(record.fileUrl)
    setPreviewUrl(url)
    setPreviewType(record.materialType === 'video' ? 'video' : 'ppt')
    setPreviewVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/material/${id}`)
      message.success('删除成功')
      loadData()
    } catch { message.error('删除失败') }
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/material/page', {
        params: { pageNum, pageSize, knowledgePointId: selectedKpId || undefined }
      }) as any
      const records: Material[] = res?.records || (res?.data?.records) || []
      // 确保knowledgePointId字段存在（兼容旧数据）
      const recordsExt = records.map((r: any) => ({
        ...r,
        knowledgePointId: r.knowledgePointId ?? r.knowledge_point_id ?? undefined,
      }))
      setData(recordsExt)
    } catch { message.error('加载数据失败') }
    finally { setLoading(false) }
  }

  const loadKpList = async () => {
    try {
      const res = await request.get('/knowledge-point/list') as any
      setKpList(res || [])
    } catch { /* ignore */ }
  }

  const loadCourses = async () => {
    try {
      const res = await request.get('/course/list') as any
      setCourseList(res?.records || res || [])
    } catch { message.error('加载课程列表失败') }
  }

  const handleOpenPptModal = () => {
    loadCourses()
    setPptModalVisible(true)
  }

  const handleGeneratePpt = async () => {
    if (!selectedCourseId) { message.warning('请选择课程'); return }
    setGenerating(true)
    try {
      const values = await pptForm.validateFields()
      const res = await request.post('/material/ppt/generate', {
        courseId: selectedCourseId,
        template: pptTemplate,
        modelName: selectedModel || undefined,
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
        setPptModalVisible(false)
        pptForm.resetFields()
        loadData()
      } else {
        message.error(res?.message || 'PPT生成失败')
      }
    } catch (e: any) { message.error(e?.response?.data?.message || 'PPT生成失败') }
    finally { setGenerating(false) }
  }

  useEffect(() => { loadData() }, [selectedKpId])
  useEffect(() => { loadKpList() }, [])

  const pptCount = data.filter(m => m.materialType === 'ppt').length
  const videoCount = data.filter(m => m.materialType === 'video').length
  const generatedCount = data.filter(m => m.status === 'generated').length
  const total = data.length

  const formatSize = (size: number) => {
    if (!size) return '-'
    if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
    return (size / (1024 * 1024)).toFixed(1) + ' MB'
  }

  // 按知识点分组
  const grouped: GroupedMaterials[] = (() => {
    const map = new Map<number | string, GroupedMaterials>()
    for (const mat of data) {
      const kpId = mat.knowledgePointId ?? '__none__'
      if (!map.has(kpId)) {
        const kp = kpList.find(k => k.id === mat.knowledgePointId)
        map.set(kpId, {
          knowledgePointId: mat.knowledgePointId,
          knowledgePointName: kp
            ? `${kp.subject || ''}·${kp.grade || ''}·${kp.content?.substring(0, 20) || ''}`
            : (kpId === '__none__' ? '未分类教材' : `知识点#${kpId}`),
          materials: [],
        })
      }
      map.get(kpId)!.materials.push(mat)
    }
    return Array.from(map.values())
  })()

  const renderMaterialCard = (material: Material) => (
    <Col span={8} key={material.id}>
      <Card
        className="relative overflow-hidden hover:shadow-xl transition-all duration-300 group"
        styles={{ body: { padding: 0 } }}
        cover={
          <div
            className="h-32 relative flex items-center justify-center"
            style={{
              background: material.materialType === 'video'
                ? 'linear-gradient(135deg, #1e3a5f 0%, #0f2744 100%)'
                : 'linear-gradient(135deg, #fa8c16 0%, #d97706 100%)',
            }}
          >
            <div className="absolute inset-0 opacity-10"
              style={{
                backgroundImage: 'radial-gradient(circle at 25% 25%, white 0%, transparent 50%), radial-gradient(circle at 75% 75%, white 0%, transparent 50%)',
              }}
            />
            {material.materialType === 'video' ? (
              <VideoCameraOutlined className="text-white text-4xl opacity-80" />
            ) : (
              <FileTextOutlined className="text-white text-4xl opacity-80" />
            )}
            <div className="absolute top-3 right-3">
              {material.status === 'generated' ? (
                <Badge status="success" text={<span className="text-white text-xs drop-shadow">就绪</span>} />
              ) : (
                <Badge status="processing" text={<span className="text-white text-xs drop-shadow">生成中</span>} />
              )}
            </div>
            {material.fileUrl && material.status === 'generated' && (
              <div
                className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3"
                onClick={() => handlePreview(material)}
              >
                <Button type="primary" icon={<EyeOutlined />} shape="circle" size="large"
                  onClick={(e) => { e.stopPropagation(); handlePreview(material) }} />
                <Button type="default" icon={<DownloadOutlined />} shape="circle" size="large" ghost
                  onClick={(e) => { e.stopPropagation(); window.open(toProxyUrl(material.fileUrl), '_blank') }} />
              </div>
            )}
          </div>
        }
        actions={[
          <Tooltip title="预览" key="preview">
            {material.fileUrl && material.status === 'generated'
              ? <EyeOutlined key="preview" onClick={() => handlePreview(material)} />
              : <span key="preview" className="text-gray-300 cursor-not-allowed"><EyeOutlined /></span>
            }
          </Tooltip>,
          <Tooltip title="下载" key="download">
            {material.fileUrl
              ? <DownloadOutlined key="download" onClick={() => window.open(toProxyUrl(material.fileUrl), '_blank')} />
              : <span key="download" className="text-gray-300 cursor-not-allowed"><DownloadOutlined /></span>
            }
          </Tooltip>,
          <Popconfirm title="确定删除？" key="delete" onConfirm={() => handleDelete(material.id)}>
            <DeleteOutlined key="delete" />
          </Popconfirm>,
        ]}
      >
        <div className="p-3">
          <div className="flex items-start justify-between gap-2 mb-1">
            <Tag color={material.materialType === 'video' ? 'blue' : 'orange'} className="text-xs">
              {material.materialType === 'video' ? '视频' : 'PPT'}
            </Tag>
            <span className="text-gray-400 text-xs">{formatSize(material.fileSize)}</span>
          </div>
          <h3 className="font-semibold text-gray-800 text-sm mb-1 line-clamp-2">
            {material.title || '未命名教材'}
          </h3>
          <p className="text-gray-400 text-xs">#{material.id} · {material.createdAt?.substring(0, 10)}</p>
        </div>
      </Card>
    </Col>
  )

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">教材中心</h2>
          <p className="text-gray-400 text-sm mt-1">统一管理所有PPT课件和教学视频</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} size="large" onClick={handleOpenPptModal}
          className="shadow-lg">
          生成PPT
        </Button>
      </div>

      {/* Stats */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-orange-50 to-orange-100 border-0 shadow-sm">
            <Statistic title={<span className="text-orange-600 font-medium">PPT总数</span>} value={pptCount}
              prefix={<FileTextOutlined className="text-orange-500" />} valueStyle={{ color: '#fa8c16', fontSize: 26 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-blue-50 to-blue-100 border-0 shadow-sm">
            <Statistic title={<span className="text-blue-600 font-medium">视频总数</span>} value={videoCount}
              prefix={<VideoCameraOutlined className="text-blue-500" />} valueStyle={{ color: '#1890ff', fontSize: 26 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-green-50 to-green-100 border-0 shadow-sm">
            <Statistic title={<span className="text-green-600 font-medium">已生成</span>} value={generatedCount}
              prefix={<CheckCircleOutlined className="text-green-500" />} valueStyle={{ color: '#52c41a', fontSize: 26 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-purple-50 to-purple-100 border-0 shadow-sm">
            <Statistic title={<span className="text-purple-600 font-medium">教材总数</span>} value={total}
              prefix={<PictureOutlined className="text-purple-500" />} valueStyle={{ color: '#722ed1', fontSize: 26 }} />
          </Card>
        </Col>
      </Row>

      {/* 知识点筛选 */}
      <Card size="small" className="mb-6 shadow-sm">
        <div className="flex items-center gap-3">
          <span className="text-gray-500 text-sm font-medium shrink-0">筛选知识点：</span>
          <Select
            allowClear
            style={{ width: 320 }}
            placeholder="全部知识点"
            value={selectedKpId}
            onChange={(v) => setSelectedKpId(v ?? null)}
            options={kpList.map(kp => ({
              value: kp.id,
              label: `${kp.subject || ''} · ${kp.grade || ''} · ${kp.content?.substring(0, 25) || ''}`,
            }))}
            loading={loading}
          />
          <span className="text-gray-400 text-xs">
            {data.length > 0 ? `共 ${data.length} 条教材` : ''}
          </span>
        </div>
      </Card>

      {/* Material Grid by Group */}
      {data.length === 0 && !loading ? (
        <Card className="text-center py-16 shadow-sm">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={
            <span className="text-gray-400">还没有教材，请先生成PPT或视频</span>
          } />
        </Card>
      ) : (
        <div className="space-y-8">
          {grouped.map(group => (
            <div key={group.knowledgePointId ?? '__none__'}>
              {/* Group Header */}
              <div className="flex items-center gap-2 mb-3">
                <FolderOutlined className="text-indigo-500" />
                <h3 className="font-semibold text-gray-700 text-base">
                  {group.knowledgePointName}
                </h3>
                <Tag color="purple" className="text-xs">{group.materials.length}条</Tag>
              </div>
              {/* Cards Row */}
              <Row gutter={[20, 20]}>
                {group.materials.map(mat => renderMaterialCard(mat))}
              </Row>
            </div>
          ))}
        </div>
      )}

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
                ref={iframeRef}
                id="ppt-preview-iframe"
                title="PPT Preview"
                src={pdfSrc}
                style={{ width: '100%', height: 600, border: 'none', display: 'block' }}
              />
            </div>
          )}
        </div>
      </Modal>

      {/* Generate PPT Modal */}
      <Modal
        title={<span className="text-lg font-semibold flex items-center gap-2">
          <FileTextOutlined className="text-orange-500" />生成PPT课件
        </span>}
        open={pptModalVisible}
        onCancel={() => { setPptModalVisible(false); pptForm.resetFields() }}
        footer={null}
        destroyOnClose
        width={520}
      >
        <div className="py-4 space-y-4">
          <div>
            <label className="block font-medium text-gray-700 mb-2">选择课程</label>
            <Select
              style={{ width: '100%' }}
              placeholder="请选择课程"
              value={selectedCourseId}
              onChange={setSelectedCourseId}
              size="large"
              options={courseList.map(c => ({ value: c.id, label: c.title || `课程#${c.id}` }))}
            />
          </div>
          <div>
            <label className="block font-medium text-gray-700 mb-2">模板风格</label>
            <Select
              style={{ width: '100%' }}
              value={pptTemplate}
              onChange={setPptTemplate}
              size="large"
              options={PPT_TEMPLATES}
            />
          </div>
          <div>
            <label className="block font-medium text-gray-700 mb-2">AI模型</label>
            <Select
              style={{ width: '100%' }}
              value={selectedModel}
              onChange={setSelectedModel}
              size="large"
              options={AI_MODELS}
              placeholder="自动选择可用模型"
            />
          </div>
          <Form form={pptForm} layout="vertical" size="small">
            <div className="bg-gray-50 rounded-xl p-4 space-y-3">
              <p className="text-gray-500 text-sm mb-2 font-medium">📝 自定义内容要求（可选）</p>
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
                <Input.TextArea placeholder="例如：需要联系生活实际、加强互动设计..." rows={2} />
              </Form.Item>
            </div>
          </Form>
          <div className="flex justify-end gap-3 pt-2">
            <Button onClick={() => { setPptModalVisible(false); pptForm.resetFields() }}>取消</Button>
            <Button type="primary" icon={<FileTextOutlined />} loading={generating} onClick={handleGeneratePpt}
              className="shadow-lg">
              开始生成
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default MaterialPage
