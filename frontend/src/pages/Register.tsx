import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import request from '@/api/request'
import { useUserStore } from '@/store/userStore'
import type { LoginResponse } from '@/api/types'

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const Register = () => {
  const navigate = useNavigate()
  const { setUser, setToken } = useUserStore()
  const [loading, setLoading] = useState(false)

  const validatePassword = (_: any, value: string) => {
    if (value && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,}$/.test(value)) {
      return Promise.reject('密码至少8位，必须包含大小写字母和数字')
    }
    return Promise.resolve()
  }

  const onFinish = async (values: RegisterForm) => {
    if (values.password !== values.confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }

    setLoading(true)
    try {
      const res = await request.post<any, LoginResponse>('/auth/register', {
        username: values.username,
        email: values.email,
        password: values.password,
      })
      setToken(res.token)
      if (res.refreshToken) {
        localStorage.setItem('refresh-token', res.refreshToken)
      }
      setUser({
        id: res.userId,
        username: res.username,
        email: values.email,
        avatar: ''
      })
      message.success('注册成功')
      navigate('/')
    } catch (error: any) {
      const errorMsg = error?.response?.data?.message || '注册失败，请重试'
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600">
      <Card className="w-96 shadow-xl">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-gray-800">AI Teacher Studio</h1>
          <p className="text-gray-500 mt-2">创建新账号</p>
        </div>
        <Form
          name="register"
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
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="邮箱"
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { validator: validatePassword }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码（至少8位，包含大小写和数字）"
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="确认密码"
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
              注册
            </Button>
          </Form.Item>
          <div className="text-center mt-4">
            <span className="text-gray-500">已有账号？</span>
            <Link to="/login" className="text-blue-500 hover:text-blue-600">
              立即登录
            </Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Register