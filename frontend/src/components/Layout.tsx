import { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown } from 'antd'
import { UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined, HomeOutlined, BookOutlined, ExperimentOutlined, FolderOutlined, SettingOutlined, FileTextOutlined, CrownOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'

const { Header, Sider, Content } = Layout

const AppLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useUserStore()

  const menuItems: MenuProps['items'] = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: '首页',
    },
    {
      key: '/knowledge',
      icon: <ExperimentOutlined />,
      label: '知识点',
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
  ]

  const userMenu = {
    items: [
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        danger: true,
      },
    ],
    onClick: ({ key }: { key: string }) => {
      if (key === 'logout') {
        logout()
        navigate('/login')
      }
    },
  }

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  return (
    <Layout className="min-h-screen">
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div className="h-16 flex items-center justify-center text-white text-xl font-bold">
          {collapsed ? 'AI' : 'AI Teacher'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header className="bg-white px-4 flex items-center justify-between shadow">
          <div className="flex items-center">
            {collapsed ? (
              <MenuUnfoldOutlined className="text-xl cursor-pointer" onClick={() => setCollapsed(!collapsed)} />
            ) : (
              <MenuFoldOutlined className="text-xl cursor-pointer" onClick={() => setCollapsed(!collapsed)} />
            )}
          </div>
          <div className="flex items-center gap-4">
            <Dropdown menu={userMenu as MenuProps} placement="bottomRight">
              <Avatar icon={<UserOutlined />} className="cursor-pointer" />
            </Dropdown>
            <span className="font-medium">{user?.username || '用户'}</span>
          </div>
        </Header>
        <Content className="p-6">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppLayout
