import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Table, Button, Tag, Space, Modal, Form, Select, message, Card, Spin, Radio, Input } from 'antd'
import { FileTextOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import request from '@/api/request'

interface Question {
  type: string
  content: string
  options?: string[]
  answer: string
  explanation?: string
}

interface QuizResult {
  courseId: number
  questions: Question[]
}

const QuizPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [quizResult, setQuizResult] = useState<QuizResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [showModal, setShowModal] = useState(false)
  const [form] = Form.useForm()
  const [userAnswers, setUserAnswers] = useState<Record<number, string>>({})
  const [courseOptions, setCourseOptions] = useState<{label: string; value: number}[]>([])

  const courseId = searchParams.get('courseId')

  const loadCourses = async () => {
    try {
      const res = await request.get('/course/page', { params: { pageNum: 1, pageSize: 100 } }) as any
      const records = res?.data?.records || []
      setCourseOptions(records.map((c: any) => ({ label: c.title, value: c.id })))
    } catch { /* ignore */ }
  }

  useEffect(() => {
    if (showModal) loadCourses()
  }, [showModal])

  const columns = [
    {
      title: '题号',
      key: 'index',
      width: 70,
      render: (_: any, __: any, idx: number) => idx + 1,
    },
    {
      title: '题型',
      dataIndex: 'type',
      key: 'type',
      width: 80,
      render: (type: string) => {
        const map: Record<string, string> = { choice: '选择题', blank: '填空题', essay: '简答题' }
        return <Tag>{map[type] || type}</Tag>
      },
    },
    {
      title: '题目内容',
      dataIndex: 'content',
      key: 'content',
    },
    {
      title: '选项',
      key: 'options',
      render: (_: any, record: Question) => {
        if (record.type !== 'choice' || !record.options) return '-'
        return (
          <div style={{ fontSize: 13 }}>
            {record.options.map((opt, i) => (
              <div key={i} style={{ marginBottom: 2 }}>
                {String.fromCharCode(65 + i)}. {opt}
              </div>
            ))}
          </div>
        )
      },
    },
  ]

  const handleGenerate = async () => {
    const values = await form.validateFields()
    setGenerating(true)
    try {
      const res = await request.post('/quiz/generate', {
        courseId: values.courseId,
        difficulty: values.difficulty || 'medium',
        type: values.type || 'mixed',
        count: values.count || 5,
      }) as any
      if (res.code === 200) {
        setQuizResult(res.data)
        setShowModal(false)
        form.resetFields()
        message.success('测验生成成功')
      } else {
        message.error(res.message || '生成失败')
      }
    } catch (error: any) {
      message.error(error?.message || '生成失败')
    } finally {
      setGenerating(false)
    }
  }

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
          返回
        </Button>
        <Button
          type="primary"
          icon={<FileTextOutlined />}
          style={{ marginLeft: 8 }}
          onClick={() => setShowModal(true)}
        >
          生成新测验
        </Button>
      </div>

      <Card title="测验题目">
        {!quizResult ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#999' }}>
            还没有测验内容，请点击「生成新测验」创建
          </div>
        ) : (
          <Table
            dataSource={quizResult.questions.map((q, i) => ({ ...q, key: i }))}
            columns={columns}
            pagination={false}
            style={{ marginBottom: 24 }}
          />
        )}
      </Card>

      {quizResult && (
        <Card title="答案与解析" style={{ marginTop: 16 }}>
          {quizResult.questions.map((q, idx) => (
            <Card
              key={idx}
              size="small"
              type="inner"
              style={{ marginBottom: 12 }}
            >
              <p><strong>Q{idx + 1}:</strong> {q.content}</p>
              <p style={{ color: '#52c41a' }}><strong>答案:</strong> {q.answer}</p>
              {q.explanation && (
                <p style={{ color: '#888' }}><strong>解析:</strong> {q.explanation}</p>
              )}
            </Card>
          ))}
        </Card>
      )}

      <Modal
        title="生成测验"
        open={showModal}
        onOk={handleGenerate}
        onCancel={() => { setShowModal(false); form.resetFields() }}
        confirmLoading={generating}
      >
        <Form form={form} layout="vertical" initialValues={{ courseId: courseId ? +courseId : undefined }}>
          <Form.Item
            name="courseId"
            label="选择课程"
            rules={[{ required: true, message: '请选择课程' }]}
          >
            <Select
              options={courseOptions}
              placeholder="请选择课程"
              showSearch
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.label as any)?.toLowerCase?.().includes(input.toLowerCase()) ?? false
              }
            />
          </Form.Item>
          <Form.Item name="count" label="题目数量" initialValue={5}>
            <Select>
              {[3, 5, 10, 15, 20].map(n => (
                <Select.Option key={n} value={n}>{n} 题</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="difficulty" label="难度" initialValue="medium">
            <Radio.Group>
              <Radio.Button value="easy">简单</Radio.Button>
              <Radio.Button value="medium">中等</Radio.Button>
              <Radio.Button value="hard">困难</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item name="type" label="题型" initialValue="mixed">
            <Select>
              <Select.Option value="choice">选择题</Select.Option>
              <Select.Option value="blank">填空题</Select.Option>
              <Select.Option value="essay">简答题</Select.Option>
              <Select.Option value="mixed">混合</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default QuizPage