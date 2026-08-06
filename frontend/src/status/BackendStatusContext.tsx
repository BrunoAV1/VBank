import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { Health } from '../api/types'

type State = 'checking' | 'up' | 'down'
interface StatusValue { state: State; health: Health | null; retry: () => Promise<void> }
const BackendStatusContext = createContext<StatusValue | null>(null)

export function BackendStatusProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<State>('checking')
  const [health, setHealth] = useState<Health | null>(null)
  const retry = useCallback(async () => {
    setState('checking')
    try {
      const { data } = await api.get<Health>('/health', { timeout: 8_000 })
      setHealth(data)
      setState(data.status === 'UP' ? 'up' : 'down')
    } catch {
      setHealth(null)
      setState('down')
    }
  }, [])
  useEffect(() => { void retry() }, [retry])
  const value = useMemo(() => ({ state, health, retry }), [state, health, retry])
  return <BackendStatusContext.Provider value={value}>{children}</BackendStatusContext.Provider>
}

export function useBackendStatus() {
  const value = useContext(BackendStatusContext)
  if (!value) throw new Error('useBackendStatus deve ser usado dentro de BackendStatusProvider')
  return value
}

