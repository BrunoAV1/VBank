import { StrictMode, lazy, Suspense } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { BackendStatusProvider } from './status/BackendStatusContext'
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute'
import { AppLayout } from './components/AppLayout'
import { Loading } from './components/Feedback'
import './styles.css'

const LandingPage = lazy(() => import('./pages/LandingPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const TransferPage = lazy(() => import('./pages/TransferPage'))
const StatementPage = lazy(() => import('./pages/StatementPage'))
const KeysPage = lazy(() => import('./pages/KeysPage'))
const ProfilePage = lazy(() => import('./pages/ProfilePage'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'))
const ReceiptPage = lazy(() => import('./pages/ReceiptPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'))

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <BackendStatusProvider>
        <AuthProvider>
          <Suspense fallback={<main className="centered-page"><Loading /></main>}>
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route element={<ProtectedRoute />}>
                <Route path="/app" element={<AppLayout />}>
                  <Route index element={<DashboardPage />} />
                  <Route path="dashboard" element={<DashboardPage />} />
                  <Route path="transfer" element={<TransferPage />} />
                  <Route path="statement" element={<StatementPage />} />
                  <Route path="keys" element={<KeysPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                  <Route path="notifications" element={<NotificationsPage />} />
                  <Route path="receipt/:id" element={<ReceiptPage />} />
                  <Route element={<AdminRoute />}><Route path="admin" element={<AdminPage />} /></Route>
                </Route>
              </Route>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </Suspense>
        </AuthProvider>
      </BackendStatusProvider>
    </BrowserRouter>
  </StrictMode>,
)
