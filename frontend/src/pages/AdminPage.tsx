import { useCallback, useEffect, useState } from 'react'
import { api, problemMessage } from '../api/client'
import type { Account, Page, Transfer, User } from '../api/types'
import { Empty, ErrorMessage, Loading } from '../components/Feedback'
import { formatDate, money } from '../utils/format'

interface AdminUser { user: User; account: Account | null }
interface Audit { id: string; action: string; outcome: string; actorLabel: string; targetType?: string; targetId?: string; metadata?: string; createdAt: string }
type Tab = 'users' | 'transfers' | 'audits'

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('users')
  const [search, setSearch] = useState('')
  const [number, setNumber] = useState(0)
  const [status, setStatus] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [users, setUsers] = useState<Page<AdminUser> | null>(null)
  const [transfers, setTransfers] = useState<Page<Transfer> | null>(null)
  const [audits, setAudits] = useState<Page<Audit> | null>(null)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setError('')
    try {
      if (tab === 'users') setUsers((await api.get<Page<AdminUser>>('/admin/users', { params: { search: search || undefined, page: number, size: 20, sort: 'createdAt,desc' } })).data)
      if (tab === 'transfers') setTransfers((await api.get<Page<Transfer>>('/admin/transfers', { params: {
        search: search || undefined, status: status || undefined, minAmount: minAmount || undefined,
        maxAmount: maxAmount || undefined, from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
        to: to ? endExclusive(to) : undefined, page: number, size: 20, sort: 'createdAt,desc',
      } })).data)
      if (tab === 'audits') setAudits((await api.get<Page<Audit>>('/admin/audit-logs', { params: { search: search || undefined, page: number, size: 20, sort: 'createdAt,desc' } })).data)
    } catch (caught) { setError(problemMessage(caught)) }
  }, [tab, search, number, status, minAmount, maxAmount, from, to])
  useEffect(() => { void load() }, [load])
  const accountAction = async (account: Account, action: 'block' | 'unblock') => { try { await api.patch(`/admin/accounts/${account.id}/${action}`); await load() } catch (caught) { setError(problemMessage(caught)) } }
  const adjust = async (account: Account) => {
    const amount = window.prompt('Valor do ajuste fictício (negativo para débito):'); if (!amount) return
    const reason = window.prompt('Motivo auditável do ajuste:'); if (!reason) return
    try { await api.post(`/admin/accounts/${account.id}/adjustments`, { amount: Number(amount.replace(',', '.')), reason }); await load() } catch (caught) { setError(problemMessage(caught)) }
  }
  const content = tab === 'users' ? users : tab === 'transfers' ? transfers : audits
  return <div className="page-stack"><header className="page-heading"><div><span className="eyebrow">Acesso administrativo</span><h1>Administração</h1><p>Bloqueios e ajustes são persistidos no ledger e na auditoria.</p></div><span className="status-pill status-pill--warning">Área restrita</span></header><nav className="tabs">{(['users', 'transfers', 'audits'] as Tab[]).map((item) => <button className={tab === item ? 'active' : ''} key={item} onClick={() => { setTab(item); setSearch(''); setNumber(0); setStatus(''); setMinAmount(''); setMaxAmount(''); setFrom(''); setTo('') }}>{item === 'users' ? 'Usuários' : item === 'transfers' ? 'Transferências' : 'Auditoria'}</button>)}</nav><div className="filter-bar filter-bar--wrap"><label>Buscar<input value={search} onChange={(event) => { setSearch(event.target.value); setNumber(0) }} placeholder="Nome, e-mail ou identificador" /></label>{tab === 'transfers' ? <><label>Status<select value={status} onChange={(event) => { setStatus(event.target.value); setNumber(0) }}><option value="">Todos</option><option value="COMPLETED">Concluída</option><option value="PENDING">Pendente</option><option value="FAILED">Falhou</option><option value="REVERSED">Estornada</option></select></label><label>Valor mínimo<input type="number" min="0" step="0.01" value={minAmount} onChange={(event) => { setMinAmount(event.target.value); setNumber(0) }} /></label><label>Valor máximo<input type="number" min="0" step="0.01" value={maxAmount} onChange={(event) => { setMaxAmount(event.target.value); setNumber(0) }} /></label><label>De<input type="date" value={from} onChange={(event) => { setFrom(event.target.value); setNumber(0) }} /></label><label>Até<input type="date" value={to} onChange={(event) => { setTo(event.target.value); setNumber(0) }} /></label></> : null}</div><ErrorMessage message={error} />{!content ? <Loading /> : null}
    {tab === 'users' && users ? <section className="panel admin-list">{users.content.length === 0 ? <Empty title="Nenhum usuário" text="A busca não encontrou resultados." /> : users.content.map(({ user, account }) => <article key={user.id}><span className="avatar">{user.fullName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('')}</span><div><strong>{user.fullName}</strong><small>{user.email} · @{user.username}</small><span>{user.status} · {account?.status ?? 'sem conta'} · {account ? money.format(account.balance) : ''}</span></div>{account && account.status !== 'SYSTEM' ? <div className="admin-actions"><button onClick={() => void accountAction(account, account.status === 'ACTIVE' ? 'block' : 'unblock')}>{account.status === 'ACTIVE' ? 'Bloquear' : 'Desbloquear'}</button><button onClick={() => void adjust(account)}>Ajustar</button></div> : null}</article>)}</section> : null}
    {tab === 'transfers' && transfers ? <section className="panel admin-list">{transfers.content.map((item) => <article key={item.id}><span className="transaction-icon">↗</span><div><strong>{item.publicId}</strong><small>{item.payerName} → {item.recipientName}</small><span>{formatDate(item.createdAt)} · {item.status}</span></div><b>{money.format(item.amount)}</b></article>)}</section> : null}
    {tab === 'audits' && audits ? <section className="panel admin-list audit-list">{audits.content.map((item) => <article key={item.id}><span className="transaction-icon">◇</span><div><strong>{item.action} · {item.outcome}</strong><small>{item.actorLabel} · {formatDate(item.createdAt)}</small><span>{item.targetType} {item.targetId} {item.metadata}</span></div></article>)}</section> : null}
    {content ? <div className="pagination"><button disabled={content.first} onClick={() => setNumber((value) => value - 1)}>← Anterior</button><span>Página {content.number + 1} de {Math.max(content.totalPages, 1)}</span><button disabled={content.last} onClick={() => setNumber((value) => value + 1)}>Próxima →</button></div> : null}
  </div>
}

function endExclusive(date: string) {
  const value = new Date(`${date}T00:00:00`)
  value.setDate(value.getDate() + 1)
  return value.toISOString()
}
