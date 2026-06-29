import { createBrowserRouter, Navigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'
import Layout from '@/components/Layout'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Dashboard from '@/pages/Dashboard'
import KnowledgePage from '@/pages/KnowledgePage'
import CoursePage from '@/pages/CoursePage'
import CourseDetailPage from '@/pages/CourseDetailPage'
import MaterialPage from '@/pages/MaterialPage'
import QuizPage from '@/pages/QuizPage'
import SettingsPage from '@/pages/SettingsPage'
import PricingPage from '@/pages/PricingPage'
import LandingPage from '@/pages/LandingPage'
import TasksPage from '@/pages/TasksPage'
import WorkspacePage from '@/pages/WorkspacePage'
import NotFound from '@/pages/NotFound'

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token } = useUserStore()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <LandingPage />,
  },
  {
    path: '/app',
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
        path: 'course/:id',
        element: <CourseDetailPage />,
      },
      {
        path: 'materials',
        element: <MaterialPage />,
      },
      {
        path: 'quiz',
        element: <QuizPage />,
      },
      {
        path: 'settings',
        element: <SettingsPage />,
      },
      {
        path: 'tasks',
        element: <TasksPage />,
      },
      {
        path: 'workspace',
        element: <WorkspacePage />,
      },
      {
        path: 'pricing',
        element: <PricingPage />,
      },
    ],
  },
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/register',
    element: <Register />,
  },
  {
    path: '*',
    element: <NotFound />,
  },
])

export default router