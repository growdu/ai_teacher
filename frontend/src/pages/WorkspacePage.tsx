import { useState, useEffect } from 'react'
import { Table, Button, Tag, Space, Modal, Form, Input, Popconfirm, message, Card, Row, Col, Statistic, Descriptions } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, FolderOutlined, ReloadOutlined } from '@ant-design/icons'
import request from '@/api/request'

interface Workspace {
  id: number
  name: string
  description?: string
  settings?: string
  createdAt: string
  updatedAt: string
}

const WorkspacePage = () => {
  const [data, setData] = useState<Workspace[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [detailModal, setDetailModal] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [selectedWorkspace, setSelectedWorkspace] = useState<Workspace | null>(null)
  const [form] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/workspace/page', { params: { pageNum: 1, pageSize: 100 } }) as any
      setData(res?.data?.records || [])
    } catch {
      message.error('加载工作空间失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreate = () => {
    setEditingId(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: Workspace) => {
    setEditingId(record.id)
    form.setFieldsValue({
      name: record.name,
      description: record.description,
    })
    setModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await request.put(`/workspace/${editingId}`, values)
        message.success('更新成功')
      } else {
        await request.post('/workspace', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      form.resetFields()
      loadData()
    } catch {
      message.error(editingId ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/workspace/${id}`)
      message.success('删除成功')
      loadData()
    } catch {
      message.error('删除失败')
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_: any, record: Workspace) => (
        <Space>
          <Button type="link" icon={<FolderOutlined />} onClick={() => {
            setSelectedWorkspace(record)
            setDetailModal(true)
          }} />
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm
            title="确认删除此工作空间？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-xl font-bold">工作空间</h2>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadData}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            创建工作空间
          </Button>
        </Space>
      </div>

      <Row gutter={16} className="mb-4">
        <Col span={6}>
          <Card><Statistic title="工作空间总数" value={data.length} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="活跃空间" value={data.length} /></Card>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={false}
      />

      <Modal
        title={editingId ? '编辑工作空间' : '创建工作空间'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields() }}
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="空间名称"
            rules={[{ required: true, message: '请输入空间名称' }]}
          >
            <Input placeholder="输入工作空间名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="输入工作空间描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="工作空间详情"
        open={detailModal}
        onCancel={() => setDetailModal(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModal(false)}>关闭</Button>,
          <Button key="edit" type="primary" onClick={() => {
            setDetailModal(false)
            if (selectedWorkspace) handleEdit(selectedWorkspace)
          }}>编辑</Button>,
        ]}
        width={520}
      >
        {selectedWorkspace && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{selectedWorkspace.id}</Descriptions.Item>
            <Descriptions.Item label="名称">{selectedWorkspace.name}</Descriptions.Item>
            <Descriptions.Item label="描述">{selectedWorkspace.description || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{selectedWorkspace.createdAt}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{selectedWorkspace.updatedAt}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default WorkspacePage