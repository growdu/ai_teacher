import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'antd'
import { userStore } from '@/store/userStore'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Track if a refresh is already in progress to avoid concurrent refresh calls
let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

const processQueue = (token: string) => {
  refreshQueue.forEach(cb => cb(token))
  refreshQueue = []
}

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = userStore.getState().token
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (body && body.data !== undefined) {
      return body.data
    }
    return response.data
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while refresh is in progress
        return new Promise(resolve => {
          refreshQueue.push((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refresh-token')

      if (!refreshToken) {
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('user-storage')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const resp = await axios.post('/api/auth/refresh', { refreshToken })
        const newToken = resp.data?.data?.token
        if (newToken) {
          userStore.getState().setToken(newToken)
          localStorage.setItem('refresh-token', refreshToken) // keep same refresh token
          processQueue(newToken)
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
          }
          return request(originalRequest)
        }
      } catch (refreshError) {
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('user-storage')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 403: message.error('拒绝访问'); break
        case 404: message.error('请求资源不存在'); break
        case 500: message.error('服务器错误'); break
        default: message.error('请求失败')
      }
    } else if (error.request) {
      message.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export default request