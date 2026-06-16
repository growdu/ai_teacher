import { createBrowserRouter, Navigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'
import Layout from '@/components/Layout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import KnowledgePage from '@/pages/KnowledgePage'
import CoursePage from '@/pages/CoursePage'
import MaterialPage from '@/pages/MaterialPage'
import SettingsPage from '@/pages/SettingsPage'

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token } = useUserStore()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <Layout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: 'knowledge',
        element: <KnowledgePage />,
      },
      {
        path: 'courses',
        element: <CoursePage />,
      },
      {
        path: 'materials',
        element: <MaterialPage />,
      },
      {
        path: 'settings',
        element: <SettingsPage />,
      },
    ],
  },
])

export default router