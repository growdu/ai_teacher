import React from 'react'

interface Props {
  children: React.ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('[ErrorBoundary] React error:', error, info.componentStack)
    // Sentry is statically imported in main.tsx — captureException is always available
    if (typeof window !== 'undefined') {
      import('../sentry').then(({ captureException }) => {
        captureException(error, { extra: { componentStack: info.componentStack } })
      }).catch(() => {/* Sentry not initialized yet */})
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#f5f5f5',
          fontFamily: 'system-ui, sans-serif'
        }}>
          <div style={{
            background: 'white',
            padding: '48px 64px',
            borderRadius: 12,
            boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
            textAlign: 'center',
            maxWidth: 480
          }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
            <h2 style={{ marginBottom: 12, color: '#333' }}>页面出现错误</h2>
            <p style={{ color: '#666', marginBottom: 24 }}>
              请尝试刷新页面。如果问题持续存在，请联系管理员。
            </p>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '10px 32px',
                background: '#1890ff',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                fontSize: 15,
                cursor: 'pointer',
                marginRight: 12
              }}
            >
              刷新页面
            </button>
            <button
              onClick={() => { this.setState({ hasError: false, error: null }); window.location.href = '/' }}
              style={{
                padding: '10px 32px',
                background: 'white',
                color: '#1890ff',
                border: '1px solid #1890ff',
                borderRadius: 6,
                fontSize: 15,
                cursor: 'pointer'
              }}
            >
              返回首页
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

export default ErrorBoundary
