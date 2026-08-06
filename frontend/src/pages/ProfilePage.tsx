import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { api, problemMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/Feedback'
import { formatDate } from '../utils/format'

interface Session { id: string; deviceSummary: string; createdAt: string; expiresAt: string }

export default function ProfilePage() {
  const { user, reloadUser, logout, logoutAll } = useAuth()
  const [sessions, setSessions] = useState<Session[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [name, setName] = useState(user?.fullName ?? '')
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' })
  const [pins, setPins] = useState({ currentPin: '', newPin: '' })
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => { api.get<Session[]>('/me/sessions').then(({ data }) => setSessions(data)).catch(() => setSessions([])) }, [])
  useEffect(() => { if ((location.state as { setupPin?: boolean } | null)?.setupPin) setMessage('Conta criada. Configure agora seu PIN de 6 dígitos.') }, [location.state])
  const run = async (action: () => Promise<unknown>, success: string) => {
    setError(''); setMessage('')
    try { await action(); setMessage(success); await reloadUser() }
    catch (caught) { setError(problemMessage(caught)) }
  }
  return (
    <div className="page-stack">
      <header className="page-heading"><div><span className="eyebrow">Dados e proteção</span><h1>Perfil e segurança</h1><p>Gerencie sua conta de demonstração e as sessões ativas.</p></div></header>
      {message ? <div className="success-box" role="status">{message}</div> : null}<ErrorMessage message={error} />
      <div className="settings-grid">
        <section className="panel form-panel"><div className="panel-heading"><div><h2>Dados pessoais</h2><p>Não use CPF, CNPJ ou dados bancários reais.</p></div></div><div className="profile-summary"><span className="avatar avatar--large">{user?.fullName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('')}</span><div><strong>{user?.fullName}</strong><span>{user?.email}</span><small>@{user?.username} · desde {user ? formatDate(user.createdAt) : ''}</small></div></div><label>Nome completo<input value={name} onChange={(event) => setName(event.target.value)} /></label><button className="button button--ghost" onClick={() => void run(() => api.patch('/me', { fullName: name }), 'Dados atualizados.')}>Salvar nome</button></section>
        <section className="panel form-panel"><div className="panel-heading"><div><h2>PIN de segurança</h2><p>{user?.pinConfigured ? 'Altere o PIN usado nas transferências.' : 'Crie um PIN de exatamente 6 dígitos.'}</p></div><span className={`status-pill ${user?.pinConfigured ? '' : 'status-pill--warning'}`}>{user?.pinConfigured ? 'Configurado' : 'Pendente'}</span></div>{user?.pinConfigured ? <label>PIN atual<input type="password" inputMode="numeric" maxLength={6} value={pins.currentPin} onChange={(event) => setPins({ ...pins, currentPin: event.target.value })} /></label> : null}<label>{user?.pinConfigured ? 'Novo PIN' : 'PIN'}<input type="password" inputMode="numeric" maxLength={6} value={pins.newPin} onChange={(event) => setPins({ ...pins, newPin: event.target.value })} /></label><button className="button button--primary" onClick={() => void run(() => user?.pinConfigured ? api.patch('/me/pin', { currentPin: pins.currentPin, newPin: pins.newPin }) : api.post('/me/pin', { pin: pins.newPin }), user?.pinConfigured ? 'PIN alterado.' : 'PIN criado.')}>{user?.pinConfigured ? 'Alterar PIN' : 'Criar PIN'}</button></section>
        <section className="panel form-panel"><div className="panel-heading"><div><h2>Senha</h2><p>Ao alterar, todas as sessões são revogadas.</p></div></div><label>Senha atual<input type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} /></label><label>Nova senha<input type="password" autoComplete="new-password" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} /><small>8+ caracteres, maiúscula, minúscula e número.</small></label><button className="button button--ghost" onClick={() => void run(() => api.patch('/me/password', passwords), 'Senha alterada; faça login novamente.')}>Alterar senha</button></section>
        <section className="panel"><div className="panel-heading"><div><h2>Sessões</h2><p>Refresh tokens rotativos; valores nunca são exibidos.</p></div></div><div className="session-list">{sessions.map((session) => <article key={session.id}><span>▣</span><div><strong>{session.deviceSummary}</strong><small>Criada {formatDate(session.createdAt)} · expira {formatDate(session.expiresAt)}</small></div></article>)}</div><button className="button button--ghost button--full" onClick={() => void logoutAll()}>Sair de todos os dispositivos</button></section>
      </div>
      <section className="danger-zone"><div><h2>Bloqueio temporário</h2><p>Bloqueia a própria conta e encerra sessões. Um administrador deverá desbloqueá-la.</p></div><button className="button button--danger" onClick={async () => { if (!window.confirm('Bloquear sua conta de demonstração?')) return; try { await api.post('/me/block'); await logout(); navigate('/login') } catch (caught) { setError(problemMessage(caught)) } }}>Bloquear minha conta</button></section>
    </div>
  )
}
