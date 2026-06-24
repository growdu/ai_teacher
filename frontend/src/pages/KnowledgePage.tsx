import { useState, useEffect } from 'react'
import {
  Table, Button, Tag, Space, Modal, Form, Input, Select, message,
  Card, Row, Col, Statistic, Empty, Tooltip, Popconfirm
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined,
  FilterOutlined, EyeOutlined, BookOutlined
} from '@ant-design/icons'
import request from '@/api/request'

const { TextArea } = Input

interface KnowledgePoint {
  id: number
  subject: string
  grade: string
  content: string
  tags: string
  createdAt: string
}

const SUBJECTS = ['数学', '物理', '化学', '生物', '语文', '英语', '历史', '地理', '政治']
const GRADES = ['初一', '初二', '初三', '高一', '高二', '高三']

const TAG_COLORS = [
  'blue', 'green', 'orange', 'red', 'purple',
  'cyan', 'magenta', 'lime', 'volcano', 'geekblue'
]

const getTagColor = (tag: string) => {
  const idx = tag.charCodeAt(0) % TAG_COLORS.length
  return TAG_COLORS[idx]
}

const KnowledgePage = () => {
  const [data, setData] = useState<KnowledgePoint[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<KnowledgePoint | null>(null)
  const [detailRecord, setDetailRecord] = useState<KnowledgePoint | null>(null)
  const [form] = Form.useForm()

  // 搜索/筛选
  const [searchText, setSearchText] = useState('')
  const [filterSubject, setFilterSubject] = useState<string>('')
  const [filterGrade, setFilterGrade] = useState<string>('')

  // 分页
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const loadData = async () => {
    setLoading(true)
    try {
      const params: Record<string, any> = { pageNum: 1, pageSize: 999 }
      const res = await request.get('/knowledge-point/page', { params }) as any
      let records = res?.records || []
      // 前端筛选
      if (filterSubject) records = records.filter((r: KnowledgePoint) => r.subject === filterSubject)
      if (filterGrade) records = records.filter((r: KnowledgePoint) => r.grade === filterGrade)
      if (searchText) {
        const q = searchText.toLowerCase()
        records = records.filter((r: KnowledgePoint) =>
          r.content?.toLowerCase().includes(q) ||
          r.tags?.toLowerCase().includes(q) ||
          r.subject?.toLowerCase().includes(q)
        )
      }
      setData(records)
      setTotal(records.length)
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [filterSubject, filterGrade, searchText])

  const handleAdd = () => {
    setEditingRecord(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: KnowledgePoint) => {
    setEditingRecord(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleView = (record: KnowledgePoint) => {
    setDetailRecord(record)
    setDetailVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/knowledge-point/${id}`)
      message.success('删除成功')
      loadData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingRecord) {
        await request.put(`/knowledge-point/${editingRecord.id}`, values)
        message.success('更新成功')
      } else {
        await request.post('/knowledge-point', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch {
      message.error('操作失败')
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
      render: (id: number) => <span className="text-gray-400 text-xs">#{id}</span>,
    },
    {
      title: '科目',
      dataIndex: 'subject',
      key: 'subject',
      width: 90,
      render: (s: string) => <Tag color="blue">{s}</Tag>,
      filters: SUBJECTS.map(s => ({ text: s, value: s })),
      onFilter: (value: any) => { setFilterSubject(value); return true },
    },
    {
      title: '年级',
      dataIndex: 'grade',
      key: 'grade',
      width: 80,
      render: (g: string) => <Tag color="purple">{g}</Tag>,
    },
    {
      title: '内容摘要',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      render: (c: string) => (
        <Tooltip title={c}>
          <span className="text-gray-700 cursor-pointer" onClick={() => handleView({ content: c } as KnowledgePoint)}>
            {c?.substring(0, 60)}{c?.length > 60 ? '...' : ''}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      width: 200,
      render: (tags: string) => (
        <Space wrap size={[4, 4]}>
          {tags?.split(',').filter(Boolean).map((tag: string, i: number) => (
            <Tag key={i} color={getTagColor(tag)} className="text-xs">{tag.trim()}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: any, record: KnowledgePoint) => (
        <Space size="small">
          <Button type="text" size="small" icon={<EyeOutlined />} onClick={() => handleView(record)} />
          <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      {/* Page Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">知识点管理</h2>
          <p className="text-gray-400 text-sm mt-1">管理和维护您的教学知识点库</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} size="large" onClick={handleAdd}
          className="shadow-lg"
        >
          添加知识点
        </Button>
      </div>

      {/* Stats & Filters */}
      <Row gutter={16} className="mb-4">
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-blue-50 to-blue-100 border-0">
            <Statistic title={<span className="text-blue-600">知识点总数</span>} value={total} prefix={<BookOutlined className="text-blue-500" />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small" className="bg-gradient-to-br from-green-50 to-green-100 border-0">
            <Statistic title={<span className="text-green-600">涉及科目</span>} value={new Set(data.map(d => d.subject)).size} prefix={<FilterOutlined className="text-green-500" />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" className="h-full flex items-center">
            <Space wrap size="middle" className="w-full">
              <Input
                placeholder="搜索知识点内容..."
                prefix={<SearchOutlined className="text-gray-400" />}
                value={searchText}
                onChange={e => setSearchText(e.target.value)}
                className="w-56"
                allowClear
              />
              <Select
                placeholder="筛选科目"
                allowClear
                value={filterSubject || undefined}
                onChange={v => setFilterSubject(v || '')}
                className="w-32"
                options={SUBJECTS.map(s => ({ value: s, label: s }))}
              />
              <Select
                placeholder="筛选年级"
                allowClear
                value={filterGrade || undefined}
                onChange={v => setFilterGrade(v || '')}
                className="w-28"
                options={GRADES.map(g => ({ value: g, label: g }))}
              />
              {(searchText || filterSubject || filterGrade) && (
                <Button size="small" onClick={() => { setSearchText(''); setFilterSubject(''); setFilterGrade('') }}>
                  清空筛选
                </Button>
              )}
            </Space>
          </Card>
        </Col>
      </Row>

      {/* Table */}
      <Card styles={{ body: { padding: 0 } }}>
        <Table
          columns={columns}
          dataSource={data}
          loading={loading}
          rowKey="id"
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <span className="text-gray-400">
                    {searchText || filterSubject || filterGrade
                      ? '没有匹配的结果，请调整筛选条件'
                      : '还没有知识点，点击上方按钮添加'}
                  </span>
                }
              >
                {(searchText || filterSubject || filterGrade) && (
                  <Button onClick={() => { setSearchText(''); setFilterSubject(''); setFilterGrade('') }}>
                    清空筛选
                  </Button>
                )}
              </Empty>
            )
          }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => { setPageNum(p); setPageSize(ps || 10) },
          }}
        />
      </Card>

      {/* Create/Edit Modal */}
      <Modal
        title={
          <span className="text-lg">
            {editingRecord ? '编辑知识点' : '添加知识点'}
          </span>
        }
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={560}
        destroyOnClose
        okText={editingRecord ? '保存修改' : '创建知识点'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" className="mt-4">
          <div className="grid grid-cols-2 gap-4">
            <Form.Item name="subject" label="科目" rules={[{ required: true, message: '请选择科目' }]}>
              <Select size="large" placeholder="选择科目" options={SUBJECTS.map(s => ({ value: s, label: s }))} />
            </Form.Item>
            <Form.Item name="grade" label="年级" rules={[{ required: true, message: '请选择年级' }]}>
              <Select size="large" placeholder="选择年级" options={GRADES.map(g => ({ value: g, label: g }))} />
            </Form.Item>
          </div>
          <Form.Item
            name="content"
            label="知识点内容"
            rules={[{ required: true, message: '请输入知识点内容' }]}
          >
            <TextArea rows={5} placeholder="详细描述这个知识点的核心内容..." showCount maxLength={2000} />
          </Form.Item>
          <Form.Item name="tags" label="标签（用逗号分隔多个标签）">
            <Input placeholder="如：重点, 难点, 考点" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Detail Modal */}
      <Modal
        title="知识点详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={
          <Button onClick={() => setDetailVisible(false)}>关闭</Button>
        }
        width={600}
      >
        {detailRecord && (
          <div className="space-y-4">
            <div className="flex gap-3">
              <Tag color="blue" className="text-sm">{detailRecord.subject}</Tag>
              <Tag color="purple" className="text-sm">{detailRecord.grade}</Tag>
            </div>
            <div>
              <h4 className="text-gray-500 text-sm mb-2">内容</h4>
              <div className="bg-gray-50 rounded-lg p-4 text-gray-700 leading-relaxed whitespace-pre-wrap">
                {detailRecord.content}
              </div>
            </div>
            {detailRecord.tags && (
              <div>
                <h4 className="text-gray-500 text-sm mb-2">标签</h4>
                <Space wrap size={[4, 4]}>
                  {detailRecord.tags.split(',').filter(Boolean).map((tag: string, i: number) => (
                    <Tag key={i} color={getTagColor(tag)}>{tag.trim()}</Tag>
                  ))}
                </Space>
              </div>
            )}
            <div className="text-gray-400 text-xs">创建于 {detailRecord.createdAt}</div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default KnowledgePage
