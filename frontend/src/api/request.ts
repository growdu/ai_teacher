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

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Use raw store getState() to get the latest token synchronously
    const token = userStore.getState().token
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    // Unwrap { code, message, data } → return only the data payload
    const body = response.data
    if (body && body.data !== undefined) {
      return body.data
    }
    return response.data
  },
  (error: AxiosError) => {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          message.error('未授权，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求资源不存在')
          break
        case 500:
          message.error('服务器错误')
          break
        default:
          message.error('请求失败')
      }
    } else if (error.request) {
      message.error('网络连接失败')
    } else {
      message.error('请求配置错误')
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