import { useState } from 'react'
import { Outlet, Menu, Layout, Avatar, Dropdown, Button } from 'antd'
import { UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'

const { Header, Sider, Content } = Layout

const Layout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const { user, logout } = useUserStore()

  const menuItems = [
    {
      key: '/',
      label: '首页',
    },
    {
      key: '/students',
      label: '学生管理',
    },
    {
      key: '/courses',
      label: '课程管理',
    },
    {
      key: '/analytics',
      label: '数据分析',
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
  }

  const handleMenuClick = (key: string) => {
    navigate(key)
  }

  const handleUserMenuClick = ({ key }: { key: string }) => {
    if (key === 'logout') {
      logout()
      navigate('/login')
    }
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
          defaultSelectedKeys={['/']}
          items={menuItems}
          onClick={({ key }) => handleMenuClick(key)}
        />
      </Sider>
      <Layout>
        <Header className="bg-white px-4 flex items-center justify-between">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <Dropdown menu={userMenu} onClick={handleUserMenuClick}>
            <div className="flex items-center cursor-pointer">
              <Avatar icon={<UserOutlined />} src={user?.avatar} />
              <span className="ml-2">{user?.username || '用户'}</span>
            </div>
          </Dropdown>
        </Header>
        <Content className="bg-gray-50">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default Layout