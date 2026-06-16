import { useState, useEffect } from 'react'
import { Tabs, Card, Table, Button, Tag, Space, Modal, Form, Input, Switch, Select, message, Popconfirm } from 'antd'
import { PlusOutlined, ReloadOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '@/api/request'

interface AiConfig {
  id: number
  provider: string
  model: string
  enabled: boolean
  priority: number
  createdAt: string
}

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState('ai')
  const [aiConfigs, setAiConfigs] = useState<AiConfig[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()

  const columns = [
    {
      title: 'Provider',
      dataIndex: 'provider',
      key: 'provider',
    },
    {
      title: 'Model',
      dataIndex: 'model',
      key: 'model',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'success' : 'default'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: AiConfig) => (
        <Space>
          <Button
            type="link"
            icon={<ReloadOutlined />}
            onClick={() => handleReload(record.id)}
          >
            重载
          </Button>
          <Popconfirm
            title="确认删除这个配置？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleAdd = () => {
    form.resetFields()
    setModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      await request.post('/ai-config', values)
      message.success('添加成功')
      setModalVisible(false)
      loadAiConfigs()
    } catch (error) {
      message.error('添加失败')
    }
  }

  const handleReload = async (id: number) => {
    try {
      await request.post('/ai-config/reload', { id })
      message.success('重载成功')
      loadAiConfigs()
    } catch (error) {
      message.error('重载失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await request.delete(`/ai-config/${id}`)
      message.success('删除成功')
      loadAiConfigs()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const loadAiConfigs = async () => {
    setLoading(true)
    try {
      const res = await request.get('/ai-config/list')
      setAiConfigs(res.data || [])
    } catch (error) {
      message.error('加载配置失败')
    } finally {
      setLoading(false)
    }
  }

  const checkProviderStatus = async () => {
    try {
      const res = await request.get('/ai-config/status')
      if (res.data) {
        message.info(`当前可用 Provider: ${res.data.availableProviders?.join(', ') || '无'}`)
      }
    } catch (error) {
      message.error('检查状态失败')
    }
  }

  useEffect(() => {
    if (activeTab === 'ai') {
      loadAiConfigs()
    }
  }, [activeTab])

  const tabItems = [
    {
      key: 'ai',
      label: 'AI配置',
      children: (
        <>
          <div className="flex justify-between mb-4">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              添加AI配置
            </Button>
            <Button icon={<CheckCircleOutlined />} onClick={checkProviderStatus}>
              检查状态
            </Button>
          </div>
          <Table
            columns={columns}
            dataSource={aiConfigs}
            loading={loading}
            rowKey="id"
            pagination={false}
          />
        </>
      ),
    },
    {
      key: 'profile',
      label: '个人信息',
      children: (
        <Card title="个人信息设置">
          <Form layout="vertical">
            <Form.Item label="用户名">
              <Input placeholder="输入用户名" />
            </Form.Item>
            <Form.Item label="邮箱">
              <Input placeholder="输入邮箱" />
            </Form.Item>
            <Form.Item label="头像">
              <Input placeholder="头像URL" />
            </Form.Item>
            <Form.Item>
              <Button type="primary">保存</Button>
            </Form.Item>
          </Form>
        </Card>
      ),
    },
    {
      key: 'workspace',
      label: '工作空间',
      children: (
        <Card title="工作空间设置">
          <Form layout="vertical">
            <Form.Item label="空间名称">
              <Input placeholder="输入空间名称" />
            </Form.Item>
            <Form.Item label="描述">
              <Input.TextArea rows={3} placeholder="输入描述" />
            </Form.Item>
            <Form.Item>
              <Button type="primary">保存</Button>
            </Form.Item>
          </Form>
        </Card>
      ),
    },
  ]

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">设置</h2>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

      <Modal
        title="添加AI配置"
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={500}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="provider"
            label="Provider"
            rules={[{ required: true, message: '请选择Provider' }]}
          >
            <Select
              options={[
                { value: 'openai', label: 'OpenAI' },
                { value: 'claude', label: 'Claude' },
                { value: 'qwen', label: 'Qwen' },
                { value: 'aliyun_tts', label: '阿里云TTS' },
              ]}
              placeholder="选择AI Provider"
            />
          </Form.Item>
          <Form.Item
            name="model"
            label="模型"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="如: gpt-4o, claude-3-5-sonnet" />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key">
            <Input.Password placeholder="输入API Key" />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL">
            <Input placeholder="可选，自定义API地址" />
          </Form.Item>
          <Form.Item name="priority" label="优先级" initialValue={0}>
            <Select
              options={[
                { value: 0, label: '0 - 最低' },
                { value: 1, label: '1 - 低' },
                { value: 2, label: '2 - 中' },
                { value: 3, label: '3 - 高' },
                { value: 4, label: '4 - 最高' },
              ]}
            />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked" initialValue={true}>
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default SettingsPage