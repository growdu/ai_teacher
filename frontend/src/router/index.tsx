import { createBrowserRouter, Navigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'
import Layout from '@/components/Layout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'

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
    ],
  },
])

export default router