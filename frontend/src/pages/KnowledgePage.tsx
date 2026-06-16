import { useState } from 'react'
import { Table, Button, Tag, Space, Modal, Form, Input, Select, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
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

const KnowledgePage = () => {
  const [data, setData] = useState<KnowledgePoint[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<KnowledgePoint | null>(null)
  const [form] = Form.useForm()

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '科目',
      dataIndex: 'subject',
      key: 'subject',
      width: 100,
    },
    {
      title: '年级',
      dataIndex: 'grade',
      key: 'grade',
      width: 80,
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string) => (
        <>
          {tags?.split(',').map((tag: string, i: number) => (
            <Tag key={i} color="blue">{tag.trim()}</Tag>
          ))}
        </>
      ),
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
      render: (_: any, record: KnowledgePoint) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
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

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '删除后无法恢复，确定要删除吗？',
      onOk: async () => {
        try {
          await request.delete(`/knowledge-point/${id}`)
          message.success('删除成功')
          loadData()
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
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
    } catch (error) {
      message.error('操作失败')
    }
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/knowledge-point/list')
      setData(res.data || [])
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  // Initial load
  useState(() => {
    loadData()
  })

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-xl font-bold">知识点管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          添加知识点
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingRecord ? '编辑知识点' : '添加知识点'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="subject"
            label="科目"
            rules={[{ required: true, message: '请输入科目' }]}
          >
            <Select
              options={[
                { value: '数学', label: '数学' },
                { value: '物理', label: '物理' },
                { value: '化学', label: '化学' },
                { value: '生物', label: '生物' },
                { value: '语文', label: '语文' },
                { value: '英语', label: '英语' },
              ]}
              placeholder="选择科目"
            />
          </Form.Item>
          <Form.Item
            name="grade"
            label="年级"
            rules={[{ required: true, message: '请输入年级' }]}
          >
            <Select
              options={[
                { value: '初一', label: '初一' },
                { value: '初二', label: '初二' },
                { value: '初三', label: '初三' },
                { value: '高一', label: '高一' },
                { value: '高二', label: '高二' },
                { value: '高三', label: '高三' },
              ]}
              placeholder="选择年级"
            />
          </Form.Item>
          <Form.Item
            name="content"
            label="知识点内容"
            rules={[{ required: true, message: '请输入知识点内容' }]}
          >
            <TextArea rows={4} placeholder="输入知识点内容" />
          </Form.Item>
          <Form.Item name="tags" label="标签">
            <Input placeholder="多个标签用逗号分隔" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default KnowledgePage