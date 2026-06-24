import { useState, useEffect, useRef } from 'react'
import {
  Button, Tag, Space, message, Card, Row, Col, Statistic,
  Modal, Select, Empty, Tooltip, Badge, Popconfirm,} from 'antd'
import {
  DownloadOutlined, DeleteOutlined, PlusOutlined,
  FileOutlined, VideoCameraOutlined, EyeOutlined,
  FileTextOutlined, PictureOutlined, CheckCircleOutlined
} from '@ant-design/icons'
import request from '@/api/request'
import { userStore } from '@/store/userStore'

interface Material {
  id: number
  courseId: number
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

const PPT_TEMPLATES = [
  { value: 'default', label: '默认风格' },
  { value: 'elegant', label: '典雅风格' },
  { value: 'minimal', label: '简约风格' },
  { value: 'vibrant', label: '活力风格' },
]

const MaterialPage = () => {
  const [data, setData] = useState<Material[]>([])
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [pptModalVisible, setPptModalVisible] = useState(false)
  const [courseList, setCourseList] = useState<CourseOption[]>([])
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)
  const [pptTemplate, setPptTemplate] = useState<string>('default')

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
  const [total, setTotal] = useState(0)

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
      const res = await request.get('/material/page', { params: { pageNum, pageSize } }) as any
      const records = res?.records || (res?.data?.records) || []
      const totalVal = res?.total ?? (res?.data?.total ?? 0)
      setData(records)
      setTotal(totalVal)
    } catch { message.error('加载数据失败') }
    finally { setLoading(false) }
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
      await request.post('/material/ppt/generate', { courseId: selectedCourseId, template: pptTemplate })
      message.success('PPT生成成功')
      setPptModalVisible(false)
      loadData()
    } catch (e: any) { message.error(e?.response?.data?.message || 'PPT生成失败') }
    finally { setGenerating(false) }
  }

  useEffect(() => { loadData() }, [])

  const pptCount = data.filter(m => m.materialType === 'ppt').length
  const videoCount = data.filter(m => m.materialType === 'video').length
  const generatedCount = data.filter(m => m.status === 'generated').length

  const formatSize = (size: number) => {
    if (!size) return '-'
    if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
    return (size / (1024 * 1024)).toFixed(1) + ' MB'
  }

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

      {/* Material Grid */}
      {data.length === 0 && !loading ? (
        <Card className="text-center py-16 shadow-sm">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={
            <span className="text-gray-400">还没有教材，请先生成PPT或视频</span>
          } />
        </Card>
      ) : (
        <Row gutter={[20, 20]}>
          {data.map(material => (
            <Col span={8} key={material.id}>
              <Card
                className="relative overflow-hidden hover:shadow-xl transition-all duration-300 group"
                styles={{ body: { padding: 0 } }}
                cover={
                  <div
                    className="h-36 relative flex items-center justify-center"
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
                      <VideoCameraOutlined className="text-white text-5xl opacity-80" />
                    ) : (
                      <FileTextOutlined className="text-white text-5xl opacity-80" />
                    )}
                    {/* Status badge */}
                    <div className="absolute top-3 right-3">
                      {material.status === 'generated' ? (
                        <Badge status="success" text={<span className="text-white text-xs drop-shadow">就绪</span>} />
                      ) : (
                        <Badge status="processing" text={<span className="text-white text-xs drop-shadow">生成中</span>} />
                      )}
                    </div>
                    {/* Hover overlay */}
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
                <div className="p-4">
                  <div className="flex items-start justify-between gap-2 mb-2">
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
          ))}
        </Row>
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
        onCancel={() => setPptModalVisible(false)}
        footer={null}
        destroyOnClose
        width={480}
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
          <div className="flex justify-end gap-3 pt-2">
            <Button onClick={() => setPptModalVisible(false)}>取消</Button>
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
