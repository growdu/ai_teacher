export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  role: string
  userId: number
  refreshToken?: string
}

export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  createdAt?: string
}