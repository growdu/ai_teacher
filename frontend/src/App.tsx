import { RouterProvider } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import router from './router'
import ErrorBoundary from './components/ErrorBoundary'

function App() {
  return (
    <ErrorBoundary>
      <ConfigProvider
        theme={{
          token: {
            colorPrimary: '#1890ff',
          },
        }}
      >
        <RouterProvider router={router} />
      </ConfigProvider>
    </ErrorBoundary>
  )
}

export default App