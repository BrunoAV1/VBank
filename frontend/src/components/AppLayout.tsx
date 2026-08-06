import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { SandboxNotice } from './SandboxNotice'

const primaryLinks = [
  ['/app/dashboard', '⌂', 'Visão geral'],
  ['/app/transfer', '↗', 'Transferir'],
  ['/app/statement', '≡', 'Extrato'],
  ['/app/keys', '⌘', 'Chaves'],
  ['/app/notifications', '◌', 'Notificações'],
  ['/app/profile', '◇', 'Perfil e segurança'],
] as const

export function AppLayout() {
  const { user, logout } = useAuth()
  const initials = user?.fullName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="brand" to="/app/dashboard"><span className="brand-mark">V</span><span>VBank <em>Sandbox</em></span></NavLink>
        <nav aria-label="Navegação principal">
          {primaryLinks.map(([to, icon, label]) => <NavLink key={to} to={to}><span aria-hidden="true">{icon}</span>{label}</NavLink>)}
          {user?.roles.includes('ADMIN') ? <NavLink to="/app/admin"><span aria-hidden="true">▦</span>Administração</NavLink> : null}
        </nav>
        <div className="sidebar-user">
          <span className="avatar" aria-hidden="true">{initials}</span>
          <span><strong>{user?.fullName}</strong><small>@{user?.username}</small></span>
          <button type="button" className="icon-button" onClick={() => void logout()} aria-label="Sair da conta">↪</button>
        </div>
      </aside>
      <div className="app-column">
        <header className="mobile-header">
          <NavLink className="brand" to="/app/dashboard"><span className="brand-mark">V</span>VBank</NavLink>
          <span className="avatar" aria-label={`Usuário ${user?.fullName}`}>{initials}</span>
        </header>
        <SandboxNotice compact />
        <main className="app-main" id="main-content"><Outlet /></main>
        <footer className="app-footer">VBank Sandbox · MIT · nenhum valor financeiro real</footer>
        <nav className="bottom-nav" aria-label="Navegação móvel">
          {primaryLinks.slice(0, 5).map(([to, icon, label]) => <NavLink key={to} to={to} aria-label={label}><span>{icon}</span><small>{label.split(' ')[0]}</small></NavLink>)}
        </nav>
      </div>
    </div>
  )
}
