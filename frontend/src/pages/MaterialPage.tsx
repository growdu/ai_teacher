import { useState, useEffect } from 'react'
import { Table, Button, Tag, Space, message, Card, Row, Col, Statistic, Modal, Form, Select, Progress } from 'antd'
import { DownloadOutlined, DeleteOutlined, PlusOutlined, FileOutlined, VideoCameraOutlined, FilePdfOutlined } from '@ant-design/icons'
import request from '@/api/request'

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

  // 分页状态
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

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
      width: 150,
      render: (_: any, record: Material) => (
        <Space>
          {record.fileUrl && (
            <Button
              type="link"
              icon={<DownloadOutlined />}
              onClick={() => window.open(record.fileUrl, '_blank')}
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
      })
      setData(res.data?.records || [])
      setTotal(res.data?.total || 0)
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  const loadCourses = async () => {
    try {
      const res = await request.get('/course/list')
      setCourseList(res || [])
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