import { useState, useEffect } from 'react'
import { Tabs, Card, Table, Button, Tag, Space, Modal, Form, Input, Switch, Select, message, Popconfirm } from 'antd'
import { PlusOutlined, ReloadOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '@/api/request'
import { useUserStore } from '@/store/userStore'

interface AiConfig {
  id: number
  provider: string
  model: string
  enabled: boolean
  priority: number
  createdAt: string
}

interface UserProfile {
  id: number
  username: string
  email: string
  phone?: string
  avatar?: string
}

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState('ai')
  const [aiConfigs, setAiConfigs] = useState<AiConfig[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()

  // Profile state
  const [profileLoading, setProfileLoading] = useState(false)
  const [passwordModalVisible, setPasswordModalVisible] = useState(false)
  const { user, setUser } = useUserStore()

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
          <Button type="link" icon={<ReloadOutlined />} onClick={handleReload}>
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

  const handleReload = async () => {
    try {
      await request.post('/ai-config/reload', {})
      message.success('重载成功')
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
        const { llmProvider, ttsProvider, llmAvailable, ttsAvailable } = res.data
        const parts = []
        if (llmAvailable) parts.push(`LLM: ${llmProvider || '未知'}`)
        if (ttsAvailable) parts.push(`TTS: ${ttsProvider || '未知'}`)
        message.info(parts.length ? `当前可用: ${parts.join(' | ')}` : '无可用 Provider')
      }
    } catch (error) {
      message.error('检查状态失败')
    }
  }

  // Load user profile
  const loadProfile = async () => {
    setProfileLoading(true)
    try {
      const res = await request.get('/user/profile')
      if (res.data) {
        const profileData: UserProfile = res.data
        profileForm.setFieldsValue({
          username: profileData.username,
          email: profileData.email,
          phone: profileData.phone || '',
          avatar: profileData.avatar || '',
        })
      }
    } catch (error) {
      message.error('加载个人信息失败')
    } finally {
      setProfileLoading(false)
    }
  }

  // Save profile handler
  const handleSaveProfile = async () => {
    try {
      const values = await profileForm.validateFields()
      await request.put('/user/profile', values)
      message.success('保存成功')
      // Update user store with new info
      if (user) {
        setUser({
          ...user,
          username: values.username,
          email: values.email,
        })
      }
      // Reload profile to ensure data is fresh
      loadProfile()
    } catch (error: any) {
      message.error(error?.message || '保存失败')
    }
  }

  // Change password handler
  const handleChangePassword = async () => {
    try {
      const values = await passwordForm.validateFields()
      // Only send oldPassword and newPassword to backend
      await request.put('/user/password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      })
      message.success('密码修改成功')
      setPasswordModalVisible(false)
      passwordForm.resetFields()
    } catch (error: any) {
      message.error(error?.message || '密码修改失败')
    }
  }

  useEffect(() => {
    if (activeTab === 'ai') {
      loadAiConfigs()
    } else if (activeTab === 'profile') {
      loadProfile()
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
          <Form form={profileForm} layout="vertical">
            <Form.Item
              label="用户名"
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input placeholder="输入用户名" />
            </Form.Item>
            <Form.Item
              label="邮箱"
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' }
              ]}
            >
              <Input placeholder="输入邮箱" />
            </Form.Item>
            <Form.Item
              label="手机号"
              name="phone"
            >
              <Input placeholder="输入手机号（可选）" />
            </Form.Item>
            <Form.Item
              label="头像"
              name="avatar"
            >
              <Input placeholder="头像URL（可选）" />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" onClick={handleSaveProfile} loading={profileLoading}>
                  保存修改
                </Button>
                <Button onClick={() => setPasswordModalVisible(true)}>
                  更换密码
                </Button>
              </Space>
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
                { value: 'openai', label: 'OpenAI (GPT-4o)' },
                { value: 'claude', label: 'Claude (Anthropic)' },
                { value: 'qwen', label: 'Qwen (通义千问)' },
                { value: 'minimax', label: 'MiniMax (海螺)' },
                { value: 'minimax_video', label: 'MiniMax Video' },
                { value: 'mock', label: 'Mock (开发/演示)' },
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
          <Form.Item name="apiKeyEncrypted" label="API Key">
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

      {/* Password Change Modal */}
      <Modal
        title="更换密码"
        open={passwordModalVisible}
        onOk={handleChangePassword}
        onCancel={() => {
          setPasswordModalVisible(false)
          passwordForm.resetFields()
        }}
        width={400}
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            name="oldPassword"
            label="旧密码"
            rules={[{ required: true, message: '请输入旧密码' }]}
          >
            <Input.Password placeholder="输入旧密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '新密码长度不能少于6位' }
            ]}
          >
            <Input.Password placeholder="输入新密码" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default SettingsPage
