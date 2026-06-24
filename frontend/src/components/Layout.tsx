import { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Button, Space, Tooltip, Badge } from 'antd'
import {
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  LogoutOutlined, HomeOutlined, BookOutlined,
  ExperimentOutlined, FolderOutlined, SettingOutlined,
  FileTextOutlined, CrownOutlined, VideoCameraOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import type { MenuProps } from 'antd'
import { Outlet } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'

const { Header, Sider, Content } = Layout

const AppLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useUserStore()

  const menuItems: MenuProps['items'] = [
    // 主导航
    {
      key: 'group:main',
      type: 'group',
      label: '内容管理',
      children: [
        {
          key: '/',
          icon: <HomeOutlined />,
          label: '首页仪表盘',
        },
        {
          key: '/knowledge',
          icon: <ExperimentOutlined />,
          label: '知识点管理',
        },
        {
          key: '/courses',
          icon: <BookOutlined />,
          label: '课程管理',
        },
        {
          key: '/materials',
          icon: <FolderOutlined />,
          label: '教材中心',
        },
        {
          key: '/quiz',
          icon: <FileTextOutlined />,
          label: '测验管理',
        },
      ],
    },
    // 工具
    {
      key: 'group:tool',
      type: 'group',
      label: '辅助工具',
      children: [
        {
          key: '/tasks',
          icon: <ThunderboltOutlined />,
          label: '任务中心',
        },
        {
          key: '/workspace',
          icon: <VideoCameraOutlined />,
          label: '工作空间',
        },
      ],
    },
    // 系统
    {
      key: 'group:system',
      type: 'group',
      label: '系统',
      children: [
        {
          key: '/settings',
          icon: <SettingOutlined />,
          label: '设置',
        },
        {
          key: '/pricing',
          icon: <CrownOutlined />,
          label: '订阅套餐',
        },
      ],
    },
  ]

  const userMenu: MenuProps = {
    items: [
      {
        key: 'profile',
        icon: <UserOutlined />,
        label: '个人资料',
        onClick: () => navigate('/settings'),
      },
      { type: 'divider' },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        danger: true,
        onClick: () => { logout(); navigate('/login') },
      },
    ],
  }

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key.startsWith('/')) navigate(key)
  }

  return (
    <Layout className="min-h-screen">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={220}
        className="shadow-xl"
        style={{
          background: 'linear-gradient(180deg, #1a1a2e 0%, #16213e 100%)',
        }}
      >
        {/* Logo */}
        <div
          className="h-16 flex items-center justify-center text-white text-lg font-bold tracking-wide relative overflow-hidden"
          style={{
            borderBottom: '1px solid rgba(255,255,255,0.06)',
          }}
        >
          {collapsed ? (
            <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-sm font-bold shadow-lg">
              AI
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-sm font-bold shadow-lg">
                AI
              </div>
              <div>
                <div className="text-sm font-bold tracking-wider">TEACHER</div>
                <div className="text-xs text-white/50 tracking-widest">STUDIO</div>
              </div>
            </div>
          )}
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{
            background: 'transparent',
            borderRight: 'none',
          }}
          className="custom-menu"
        />
      </Sider>

      <Layout>
        {/* Header */}
        <Header
          className="bg-white flex items-center justify-between shadow-sm px-6"
          style={{
            height: 64,
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <div className="flex items-center">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              className="text-base"
              style={{ width: 48, height: 48 }}
            />
            <span className="text-gray-300 text-xl ml-2">|</span>
            <span className="ml-4 text-gray-500 text-sm">
              {location.pathname === '/' && '首页仪表盘'}
              {location.pathname === '/knowledge' && '知识点管理'}
              {location.pathname === '/courses' && '课程管理'}
              {location.pathname === '/materials' && '教材中心'}
              {location.pathname === '/quiz' && '测验管理'}
              {location.pathname === '/tasks' && '任务中心'}
              {location.pathname === '/workspace' && '工作空间'}
              {location.pathname === '/settings' && '设置'}
              {location.pathname === '/pricing' && '订阅套餐'}
              {location.pathname.startsWith('/course/') && '课程详情'}
            </span>
          </div>

          <div className="flex items-center gap-4">
            {user?.username ? (
              <Dropdown menu={userMenu} placement="bottomRight" trigger={['click']}>
                <div className="flex items-center gap-3 cursor-pointer hover:bg-gray-50 rounded-lg px-3 py-1.5 transition-colors">
                  <Avatar
                    icon={<UserOutlined />}
                    style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}
                    size={36}
                  />
                  <div className="hidden sm:block">
                    <div className="text-sm font-medium text-gray-700 leading-tight">{user.username}</div>
                    <div className="text-xs text-gray-400">已登录</div>
                  </div>
                </div>
              </Dropdown>
            ) : (
              <Space size="middle">
                <Link to="/login"><Button size="small">登录</Button></Link>
                <Link to="/register"><Button type="primary" size="small">注册</Button></Link>
              </Space>
            )}
          </div>
        </Header>

        {/* Content */}
        <Content
          className="p-6 bg-gray-50"
          style={{ minHeight: 'calc(100vh - 64px)' }}
        >
          <Outlet />
        </Content>
      </Layout>

      <style>{`
        .custom-menu .ant-menu-item-selected {
          background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%) !important;
          border-right: 3px solid #667eea;
        }
        .custom-menu .ant-menu-item:hover {
          background: rgba(255,255,255,0.05) !important;
        }
        .custom-menu .ant-menu-item-group-title {
          color: rgba(255,255,255,0.35) !important;
          font-size: 11px !important;
          letter-spacing: 1px !important;
          text-transform: uppercase !important;
          padding: 8px 16px 4px !important;
        }
        .custom-menu .ant-menu-item {
          border-radius: 8px;
          margin: 2px 8px;
          height: 40px;
          line-height: 40px;
        }
      `}</style>
    </Layout>
  )
}

export default AppLayout
