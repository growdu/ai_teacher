import * as Sentry from '@sentry/react'

const dsn = import.meta.env.VITE_SENTRY_DSN as string | undefined

if (dsn) {
  Sentry.init({
    dsn,
    environment: import.meta.env.MODE,
    integrations: [
      Sentry.browserTracingIntegration(),
      Sentry.replayIntegration({
        maskAllText: false,
        blockAllMedia: false,
      }),
    ],
    tracesSampleRate: 0.1,
    replaysSessionSampleRate: import.meta.env.PROD ? 0.1 : 0,
    replaysOnErrorSampleRate: 1.0,
    ignoreErrors: [
      'ResizeObserver loop',
      'Network request failed',
      'Failed to fetch',
    ],
  })
  console.log('[Sentry] initialized')
} else {
  console.warn('[Sentry] VITE_SENTRY_DSN not set — error tracking disabled')
}

// Named export for dynamic import callers
export const captureException = (err: unknown, opts?: { extra?: Record<string, unknown> }) =>
  Sentry.captureException(err, opts)
