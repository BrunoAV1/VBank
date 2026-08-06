import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { Problem, TokenResponse } from './types'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
export const api = axios.create({ baseURL, withCredentials: true, timeout: 15_000 })
let accessToken: string | null = null
let refreshPromise: Promise<TokenResponse> | null = null

export function setAccessToken(token: string | null) { accessToken = token }

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isAuthRoute = original?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && original && !original._retried && !isAuthRoute) {
      original._retried = true
      refreshPromise ??= axios.post<TokenResponse>(`${baseURL}/auth/refresh`, {}, { withCredentials: true })
        .then(({ data }) => { setAccessToken(data.accessToken); return data })
        .finally(() => { refreshPromise = null })
      try {
        await refreshPromise
        return api(original)
      } catch {
        setAccessToken(null)
        window.dispatchEvent(new Event('vbank:session-expired'))
      }
    }
    return Promise.reject(error)
  },
)

export function problemMessage(error: unknown, fallback = 'Não foi possível concluir a operação.') {
  if (axios.isAxiosError<Problem>(error)) return error.response?.data?.detail || error.response?.data?.title || fallback
  return fallback
}

export async function refreshSession() {
  const { data } = await api.post<TokenResponse>('/auth/refresh')
  setAccessToken(data.accessToken)
  return data
}
