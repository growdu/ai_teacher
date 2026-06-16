import { useState } from 'react'
import { Table, Button, Tag, Space, Modal, Form, Input, Select, message, Card, Row, Col, Statistic } from 'antd'
import { PlusOutlined, PlayCircleOutlined, FileTextOutlined, VideoCameraOutlined } from '@ant-design/icons'
import request from '@/api/request'

const { TextArea } = Input

interface Course {
  id: number
  title: string
  status: string
  knowledgePointId: number
  createdAt: string
  outline?: any
}

const CoursePage = () => {
  const [data, setData] = useState<Course[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const [generating, setGenerating] = useState(false)

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '课程标题',
      dataIndex: 'title',
      key: 'title',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          draft: 'default',
          generating: 'processing',
          generated: 'success',
          failed: 'error',
        }
        const labelMap: Record<string, string> = {
          draft: '草稿',
          generating: '生成中',
          generated: '已生成',
          failed: '失败',
        }
        return <Tag color={colorMap[status] || 'default'}>{labelMap[status] || status}</Tag>
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
      width: 250,
      render: (_: any, record: Course) => (
        <Space>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={() => handleGenerateCourse(record.id)}
            loading={generating}
          >
            生成课程
          </Button>
          <Button icon={<FileTextOutlined />} onClick={() => handleGeneratePpt(record.id)}>
            生成PPT
          </Button>
          <Button icon={<VideoCameraOutlined />} onClick={() => handleGenerateVideo(record.id)}>
            生成视频
          </Button>
        </Space>
      ),
    },
  ]

  const handleGenerateCourse = async (courseId: number) => {
    setGenerating(true)
    try {
      const res = await request.post('/course/generate', { courseId }) as any
      if (res.code === 200) {
        message.success('课程生成成功')
        loadData()
      } else {
        message.error(res.message || '生成失败')
      }
    } catch (error) {
      message.error('生成失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleGeneratePpt = async (courseId: number) => {
    try {
      const res = await request.post('/material/ppt/generate', { courseId }) as any
      if (res.code === 200) {
        message.success('PPT生成成功')
      } else {
        message.error(res.message || 'PPT生成失败')
      }
    } catch (error) {
      message.error('PPT生成失败')
    }
  }

  const handleGenerateVideo = async (courseId: number) => {
    try {
      const res = await request.post('/material/video/generate', { courseId }) as any
      if (res.code === 200) {
        message.success('视频生成任务已创建')
        const taskId = res.data?.taskId
        if (taskId) {
          checkVideoStatus(taskId)
        }
      } else {
        message.error(res.message || '视频生成失败')
      }
    } catch (error) {
      message.error('视频生成失败')
    }
  }

  const checkVideoStatus = (taskId: number) => {
    const interval = setInterval(async () => {
      try {
        const res = await request.get(`/task/${taskId}`)
        const status = res.data?.status
        if (status === 'completed') {
          clearInterval(interval)
          message.success('视频生成完成')
          loadData()
        } else if (status === 'failed') {
          clearInterval(interval)
          message.error('视频生成失败')
        }
      } catch (error) {
        clearInterval(interval)
      }
    }, 3000)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      await request.post('/knowledge-point', values)
      message.success('创建成功')
      setModalVisible(false)
      form.resetFields()
      loadData()
    } catch (error) {
      message.error('创建失败')
    }
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/course/list')
      setData(res.data || [])
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-xl font-bold">课程管理</h2>
      </div>

      <Row gutter={16} className="mb-4">
        <Col span={6}>
          <Card>
            <Statistic title="课程总数" value={data.length} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已生成" value={data.filter(c => c.status === 'generated').length} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="生成中" value={data.filter(c => c.status === 'generating').length} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="草稿" value={data.filter(c => c.status === 'draft').length} />
          </Card>
        </Col>
      </Row>

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

export default CoursePage