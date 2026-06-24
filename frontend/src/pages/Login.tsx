import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import request from '@/api/request'
import { useUserStore } from '@/store/userStore'
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
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden"
      style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)',
      }}
    >
      {/* Animated floating orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-20 -left-20 w-96 h-96 rounded-full opacity-30 bg-white"
          style={{
            animation: 'float1 8s ease-in-out infinite',
            filter: 'blur(40px)',
          }}
        />
        <div className="absolute top-1/3 -right-20 w-80 h-80 rounded-full opacity-25 bg-yellow-300"
          style={{
            animation: 'float2 10s ease-in-out infinite',
            filter: 'blur(40px)',
          }}
        />
        <div className="absolute bottom-10 left-1/4 w-72 h-72 rounded-full opacity-20 bg-blue-300"
          style={{
            animation: 'float3 12s ease-in-out infinite',
            filter: 'blur(40px)',
          }}
        />
      </div>

      {/* Decorative grid pattern */}
      <div
        className="absolute inset-0 opacity-5 pointer-events-none"
        style={{
          backgroundImage: 'linear-gradient(rgba(255,255,255,.3) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.3) 1px, transparent 1px)',
          backgroundSize: '60px 60px',
        }}
      />

      <style>{`
        @keyframes float1 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(30px, 40px) scale(1.1); }
        }
        @keyframes float2 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(-40px, 30px) scale(0.95); }
        }
        @keyframes float3 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(20px, -30px) scale(1.05); }
        }
      `}</style>

      <Card
        className="relative z-10 w-96 shadow-2xl rounded-2xl border-0"
        style={{
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
        }}
        styles={{ body: { padding: '40px 36px' } }}
      >
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 mb-4 shadow-lg">
            <span className="text-white text-2xl font-bold">AI</span>
          </div>
          <h1 className="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            AI Teacher Studio
          </h1>
          <p className="text-gray-400 mt-2 text-sm">智能教学平台 · 登录您的账号</p>
        </div>

        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          layout="vertical"
          requiredMark={false}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined className="text-gray-400" />}
              placeholder="用户名"
              size="large"
              className="rounded-lg hover:border-indigo-400 focus:border-indigo-500"
              style={{ height: 48 }}
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined className="text-gray-400" />}
              placeholder="密码"
              size="large"
              className="rounded-lg"
              style={{ height: 48 }}
            />
          </Form.Item>

          <Form.Item className="mb-4">
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
              className="h-12 rounded-lg font-medium text-base shadow-lg shadow-indigo-500/30 hover:shadow-xl hover:shadow-indigo-500/40 transition-all"
              style={{
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                border: 'none',
              }}
            >
              登录
            </Button>
          </Form.Item>

          <div className="text-center mt-6">
            <span className="text-gray-400 text-sm">还没有账号？</span>
            <Link
              to="/register"
              className="text-indigo-500 hover:text-indigo-600 font-medium ml-1 text-sm transition-colors"
            >
              立即注册 →
            </Link>
          </div>
        </Form>
      </Card>

      {/* Bottom decoration */}
      <div className="absolute bottom-6 text-center text-white/40 text-xs">
        AI Teacher Studio · © 2024
      </div>
    </div>
  )
}

export default Login
