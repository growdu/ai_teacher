import { useState, useEffect, useRef } from 'react'
import { Table, Button, Tag, Space, message, Card, Row, Col, Statistic, Modal, Form, Select, Progress } from 'antd'
import { DownloadOutlined, DeleteOutlined, PlusOutlined, FileOutlined, VideoCameraOutlined, FilePdfOutlined, EyeOutlined } from '@ant-design/icons'
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

const MaterialPage = () => {
  const [data, setData] = useState<Material[]>([])
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const [pptModalVisible, setPptModalVisible] = useState(false)
  const [courseList, setCourseList] = useState<CourseOption[]>([])
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)
  const [pptTemplate, setPptTemplate] = useState<string>('default')

  const iframeRef = useRef<HTMLIFrameElement>(null)

  // 预览状态
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewType, setPreviewType] = useState<'video' | 'ppt'>('ppt')
  const [previewUrl, setPreviewUrl] = useState('')
  const [previewLoading, setPreviewLoading] = useState(false)

  // 预览 PDF 的 blob URL（用于 iframe）
  const [pdfSrc, setPdfSrc] = useState('')

  // 预览 Modal 打开时，fetch PDF 并转为 blob URL
  useEffect(() => {
    console.log('[PPT预览] effect triggered', { previewVisible, previewType, previewUrl })
    if (!previewVisible || previewType !== 'ppt' || !previewUrl) {
      console.log('[PPT预览] early return: missing condition')
      return
    }
    setPreviewLoading(true)
    setPdfSrc('')
    // previewUrl格式为 http://43.155.143.50/minio/ai-teacher/ppt/xxx.pptx
    // objectName只需 ppt/xxx.pptx
    const objectName = previewUrl.replace(/.*\/minio\/ai-teacher\//, '')
    const apiUrl = `/api/ppt/preview?objectName=${encodeURIComponent(objectName)}`
    console.log('[PPT预览] fetching', apiUrl)
    const token = userStore.getState().token
    console.log('[PPT预览] token:', token ? 'present' : 'MISSING')
    if (!token) { setPreviewLoading(false); return }
    fetch(apiUrl, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => {
        console.log('[PPT预览] response status:', r.status, 'type:', r.headers.get('content-type'))
        if (!r.ok) throw new Error('HTTP ' + r.status)
        return r.blob()
      })
      .then(blob => {
        console.log('[PPT预览] blob size:', blob.size, 'type:', blob.type)
        const url = URL.createObjectURL(blob)
        console.log('[PPT预览] blob url:', url)
        setPdfSrc(url)
        setPreviewLoading(false)
      })
      .catch((e) => {
        console.error('[PPT预览] fetch失败:', e)
        message.error('PPT预览加载失败')
        setPreviewLoading(false)
      })
    return () => {
      if (pdfSrc) { console.log('[PPT预览] cleanup revoke', pdfSrc); URL.revokeObjectURL(pdfSrc) }
    }
  }, [previewVisible, previewUrl, previewType])

  // 分页状态
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  // 转换 MinIO 内网 URL 为代理路径（自动使用当前域名）
  const toProxyUrl = (fileUrl: string) => {
    if (!fileUrl) return ''
    const base = window.location.origin
    return base + fileUrl.replace('http://minio:9000/ai-teacher/', '/minio/ai-teacher/')
  }

  const handlePreview = (record: Material) => {
    const url = toProxyUrl(record.fileUrl)
    setPreviewUrl(url)
    setPreviewType(record.materialType === 'video' ? 'video' : 'ppt')
    setPreviewVisible(true)
  }

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个教材吗？',
      onOk: async () => {
        try {
          await request.delete(`/material/${id}`)
          message.success('删除成功')
          loadData()
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/material/page', {
        params: { pageNum, pageSize },
      }) as any
      const records = res?.records || (res?.data?.records) || []
      const totalVal = res?.total ?? (res?.data?.total ?? 0)
      setData(records)
      setTotal(totalVal)
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  const loadCourses = async () => {
    try {
      const res = await request.get('/course/list') as any
      const list = res?.records || res || []
      setCourseList(list)
    } catch {
      message.error('加载课程列表失败')
    }
  }

  const handleOpenPptModal = () => {
    loadCourses()
    setPptModalVisible(true)
  }

  const handleGeneratePpt = async () => {
    if (!selectedCourseId) {
      message.warning('请选择课程')
      return
    }
    setGenerating(true)
    try {
      await request.post('/material/ppt/generate', {
        courseId: selectedCourseId,
        template: pptTemplate,
      })
      message.success('PPT生成成功')
      setPptModalVisible(false)
      loadData()
    } catch (error: any) {
      message.error(error.response?.data?.message || 'PPT生成失败')
    } finally {
      setGenerating(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [pageNum, pageSize])

  const pptCount = data.filter(m => m.materialType === 'ppt').length
  const videoCount = data.filter(m => m.materialType === 'video').length

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
    },
    {
      title: '类型',
      dataIndex: 'materialType',
      key: 'materialType',
      render: (type: string) => {
        const config: Record<string, { color: string; icon: React.ReactNode }> = {
          ppt: { color: 'orange', icon: <FileOutlined /> },
          video: { color: 'blue', icon: <VideoCameraOutlined /> },
          pdf: { color: 'red', icon: <FilePdfOutlined /> },
        }
        const c = config[type] || { color: 'default', icon: <FileOutlined /> }
        return <Tag color={c.color} icon={c.icon}>{type?.toUpperCase()}</Tag>
      },
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size: number) => {
        if (!size) return '-'
        if (size < 1024) return size + ' B'
        if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
        return (size / (1024 * 1024)).toFixed(1) + ' MB'
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          generated: 'success',
          generating: 'processing',
          failed: 'error',
        }
        return <Tag color={colorMap[status] || 'default'}>{status}</Tag>
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_: any, record: Material) => (
        <Space>
          {record.fileUrl && record.status === 'generated' && (
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => handlePreview(record)}
            >
              预览
            </Button>
          )}
          {record.fileUrl && (
            <Button
              type="link"
              icon={<DownloadOutlined />}
              onClick={() => window.open(toProxyUrl(record.fileUrl), '_blank')}
            >
              下载
            </Button>
          )}
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div className="flex justify-between mb-4 items-center">
        <h2 className="text-xl font-bold mb-0">教材中心</h2>
        <Button type="primary" icon={<FileOutlined />} onClick={handleOpenPptModal}>
          生成PPT
        </Button>
      </div>

      <Row gutter={16} className="mb-4">
        <Col span={8}>
          <Card>
            <Statistic title="PPT总数" value={pptCount} prefix={<FileOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="视频总数" value={videoCount} prefix={<VideoCameraOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="教材总数" value={data.length} />
          </Card>
        </Col>
      </Row>

      {generating && (
        <Card className="mb-4">
          <div className="flex items-center gap-4">
            <span>正在生成...</span>
            <Progress percent={progress} status="active" />
          </div>
        </Card>
      )}

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={{
          current: pageNum,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => {
            setPageNum(p)
            setPageSize(ps || 10)
          },
        }}
      />

      {/* 预览 Modal */}
      <Modal
        title={
          <span>
            {previewType === 'video' ? <VideoCameraOutlined style={{ color: '#1890ff', marginRight: 8 }} /> : <FileOutlined style={{ color: '#fa8c16', marginRight: 8 }} />}
            {previewType === 'video' ? '视频预览' : 'PPT预览'}
          </span>
        }
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={
          <div style={{ textAlign: 'right' }}>
            <Button icon={<DownloadOutlined />} onClick={() => window.open(previewUrl, '_blank')}>
              下载{previewType === 'video' ? '视频' : 'PPT'}
            </Button>
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
                width: '100%',
                maxHeight: '65vh',
                borderRadius: 12,
                boxShadow: '0 4px 24px rgba(0,0,0,0.15)',
                background: '#000',
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

      <Modal
        title="生成PPT"
        open={pptModalVisible}
        onCancel={() => setPptModalVisible(false)}
        footer={null}
        destroyOnClose
      >
        <div className="py-4">
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>选择课程</label>
            <Select
              style={{ width: '100%' }}
              placeholder="请选择课程"
              value={selectedCourseId}
              onChange={setSelectedCourseId}
              options={courseList.map(c => ({ value: c.id, label: c.title }))}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>模板风格</label>
            <Select
              style={{ width: '100%' }}
              value={pptTemplate}
              onChange={setPptTemplate}
              options={[
                { value: 'default', label: '默认风格' },
                { value: 'academic', label: '学术风格' },
                { value: 'modern', label: '现代风格' },
              ]}
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button onClick={() => setPptModalVisible(false)}>取消</Button>
            <Button type="primary" icon={<FileOutlined />} loading={generating} onClick={handleGeneratePpt}>
              开始生成
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default MaterialPage
