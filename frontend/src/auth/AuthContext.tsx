import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, refreshSession, setAccessToken } from '../api/client'
import type { TokenResponse, User } from '../api/types'

interface RegisterInput {
  fullName: string; email: string; username: string; password: string
  passwordConfirmation: string; acceptedTerms: boolean
}

interface AuthValue {
  user: User | null
  loading: boolean
  login: (identifier: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
  logoutAll: () => Promise<void>
  reloadUser: () => Promise<void>
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    const expireSession = () => { setAccessToken(null); setUser(null) }
    window.addEventListener('vbank:session-expired', expireSession)
    refreshSession()
      .then((session) => { if (active) setUser(session.user) })
      .catch(() => { if (active) setUser(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; window.removeEventListener('vbank:session-expired', expireSession) }
  }, [])

  const accept = useCallback((session: TokenResponse) => {
    setAccessToken(session.accessToken)
    setUser(session.user)
  }, [])

  const login = useCallback(async (identifier: string, password: string) => {
    const { data } = await api.post<TokenResponse>('/auth/login', { identifier, password })
    accept(data)
  }, [accept])

  const register = useCallback(async (input: RegisterInput) => {
    const { data } = await api.post<TokenResponse>('/auth/register', input)
    accept(data)
  }, [accept])

  const logout = useCallback(async () => {
    try { await api.post('/auth/logout') } finally { setAccessToken(null); setUser(null) }
  }, [])

  const logoutAll = useCallback(async () => {
    try { await api.post('/auth/logout-all') } finally { setAccessToken(null); setUser(null) }
  }, [])

  const reloadUser = useCallback(async () => {
    const { data } = await api.get<User>('/me')
    setUser(data)
  }, [])

  const value = useMemo(() => ({ user, loading, login, register, logout, logoutAll, reloadUser }),
    [user, loading, login, register, logout, logoutAll, reloadUser])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return value
}
