import { useState, useEffect } from 'react'
import { Table, Button, Tag, Space, Modal, Form, Input, Select, message, Card, Row, Col, Statistic } from 'antd'
import { PlusOutlined, EyeOutlined, FileTextOutlined, PlayCircleOutlined, VideoCameraOutlined } from '@ant-design/icons'
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
      width: 320,
      render: (_: any, record: Course) => (
        <Space wrap>
          <Button
            type="primary"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/course/${record.id}`)}
          >
            查看详情
          </Button>
          <Button
            icon={<FileTextOutlined />}
            onClick={() => navigate(`/quiz?courseId=${record.id}`)}
          >
            生成测验
          </Button>
          <Button icon={<PlayCircleOutlined />} onClick={() => {
            setSelectedCourseId(record.id)
            setPptTemplateModal(true)
          }}>
            生成PPT
          </Button>
          <Button icon={<VideoCameraOutlined />} onClick={() => handleGenerateVideo(record.id)}>
            生成视频
          </Button>
        </Space>
      ),
    },
  ]

  // 加载知识点列表（用于创建课程表单）
  const loadKnowledgePoints = async () => {
    try {
      const res = await request.get('/knowledge-point/page?pageNum=1&pageSize=100')
      setKnowledgePoints(res.data?.records || [])
    } catch (error) {
      message.error('加载知识点失败')
    }
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
      if (res.code === 200) {
        message.success('课程生成成功')
        setCreateModalVisible(false)
        form.resetFields()
        loadData()
      } else {
        message.error(res.message || '生成失败')
      }
    } catch (error: any) {
      if (error.errorFields) return // 表单验证失败
      message.error(error?.message || '生成失败')
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
      if (res.code === 200) {
        message.success('PPT生成成功')
        setPptTemplateModal(false)
        loadData()
      } else {
        message.error(res.message || 'PPT生成失败')
      }
    } catch (error) {
      message.error('PPT生成失败')
    } finally {
      setGenerating(false)
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

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/course/page', {
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

  useEffect(() => {
    loadData()
  }, [pageNum, pageSize])

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-xl font-bold">课程管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => {
          loadKnowledgePoints()
          setCreateModalVisible(true)
        }}>
          创建课程
        </Button>
      </div>

      <Row gutter={16} className="mb-4">
        <Col span={6}>
          <Card>
            <Statistic title="课程总数" value={total} />
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
        title="创建课程"
        open={createModalVisible}
        onOk={handleCreateCourse}
        onCancel={() => {
          setCreateModalVisible(false)
          form.resetFields()
        }}
        confirmLoading={generating}
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="knowledgePointId"
            label="选择知识点"
            rules={[{ required: true, message: '请选择知识点' }]}
          >
            <Select
              placeholder="请选择一个知识点"
              showSearch
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.children as any)?.props?.children?.toLowerCase?.().includes(input.toLowerCase()) ?? false
              }
            >
              {knowledgePoints.map(kp => (
                <Select.Option key={kp.id} value={kp.id}>
                  {kp.subject} - {kp.grade} - {kp.content.substring(0, 30)}
                  {kp.content.length > 30 ? '...' : ''}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="title" label="课程标题（可选）">
            <Input placeholder="不填则由AI自动生成" />
          </Form.Item>
          <Form.Item name="chapterCount" label="章节数量（可选）">
            <Select placeholder="默认4章" allowClear>
              {[2, 3, 4, 5, 6, 7, 8].map(n => (
                <Select.Option key={n} value={n}>{n} 章</Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="选择PPT模板"
        open={pptTemplateModal}
        onOk={handleGeneratePpt}
        confirmLoading={generating}
        onCancel={() => setPptTemplateModal(false)}
        width={480}
      >
        <div className="py-4">
          <p className="mb-3 text-gray-500">选择配色风格：</p>
          <div className="grid grid-cols-2 gap-3">
            {[
              { value: 'default',  label: '学院蓝', color: '#1F4E79', desc: '经典专业' },
              { value: 'elegant',  label: '典雅绿', color: '#1A4731', desc: '沉稳典雅' },
              { value: 'minimal',  label: '简约白', color: '#2C3E50', desc: '简洁现代' },
              { value: 'vibrant',  label: '活力橙', color: '#E67E22', desc: '活泼鲜明' },
            ].map(t => (
              <div
                key={t.value}
                onClick={() => setPptTemplate(t.value)}
                className={`cursor-pointer rounded-lg border-2 p-3 flex items-center gap-3 transition-all ${
                  pptTemplate === t.value ? 'border-blue-500 bg-blue-50' : 'border-gray-200 bg-white hover:border-blue-300'
                }`}
              >
                <div className="w-10 h-10 rounded" style={{ backgroundColor: t.color }} />
                <div>
                  <div className="font-medium text-sm">{t.label}</div>
                  <div className="text-xs text-gray-400">{t.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </Modal>

    </div>
  );
};

export default CoursePage;
