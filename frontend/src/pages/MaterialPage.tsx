import { useState, useEffect } from 'react'
import { Table, Button, Tag, Space, message, Card, Row, Col, Statistic, Modal, Form, Input, Select, Upload, Progress } from 'antd'
import { DownloadOutlined, DeleteOutlined, PlusOutlined, FileOutlined, VideoCameraOutlined, FilePdfOutlined } from '@ant-design/icons'
import { request } from '@/api/request'

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

const MaterialPage = () => {
  const [data, setData] = useState<Material[]>([])
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [progress, setProgress] = useState(0)

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
      const res = await request.get('/material/list')
      setData(res.data || [])
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const pptCount = data.filter(m => m.materialType === 'ppt').length
  const videoCount = data.filter(m => m.materialType === 'video').length

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">教材中心</h2>

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
        pagination={{ pageSize: 10 }}
      />
    </div>
  )
}

export default MaterialPage