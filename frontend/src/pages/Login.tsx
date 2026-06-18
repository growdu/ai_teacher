import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import request from '@/api/request'
import { useUserStore, userStore } from '@/store/userStore'
import type { LoginRequest, LoginResponse } from '@/api/types'

const Login = () => {
  const navigate = useNavigate()
  const { setUser, setToken } = useUserStore()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const res = await request.post<any, LoginResponse>('/auth/login', values)
      setToken(res.token)
      if (res.refreshToken) {
        localStorage.setItem('refresh-token', res.refreshToken)
      }
      setUser({
        id: res.userId,
        username: res.username,
        email: '',
        avatar: ''
      })
      message.success('登录成功')
      navigate('/')
    } catch (error) {
      message.error('登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600">
      <Card className="w-96 shadow-xl">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-gray-800">AI Teacher Studio</h1>
          <p className="text-gray-500 mt-2">智能教学平台</p>
        </div>
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              size="large"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
            >
              登录
            </Button>
          </Form.Item>
          <div className="text-center mt-4">
            <span className="text-gray-500">没有账号？</span>
            <Link to="/register" className="text-blue-500 hover:text-blue-600">
              立即注册
            </Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Login