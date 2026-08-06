import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { useBackendStatus } from '../status/BackendStatusContext'
import { Loading } from './Feedback'
import { Unavailable } from './Unavailable'

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const { state } = useBackendStatus()
  const location = useLocation()
  if (loading || state === 'checking') return <main className="centered-page"><Loading label="Restaurando sua sessão segura…" /></main>
  if (state === 'down') return <Unavailable />
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  return <Outlet />
}

export function AdminRoute() {
  const { user } = useAuth()
  return user?.roles.includes('ADMIN') ? <Outlet /> : <Navigate to="/app/dashboard" replace />
}
