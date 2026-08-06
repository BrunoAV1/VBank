import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, problemMessage } from '../api/client'
import type { DashboardData, FundingStatus } from '../api/types'
import { Empty, ErrorMessage, Loading } from '../components/Feedback'
import { useAuth } from '../auth/AuthContext'
import { formatDate, money } from '../utils/format'

export default function DashboardPage() {
  const { user } = useAuth()
  const [data, setData] = useState<DashboardData | null>(null)
  const [funding, setFunding] = useState<FundingStatus | null>(null)
  const [hidden, setHidden] = useState(false)
  const [error, setError] = useState('')
  const [fundingBusy, setFundingBusy] = useState(false)

  const load = useCallback(async () => {
    setError('')
    try {
      const [dashboard, status] = await Promise.all([
        api.get<DashboardData>('/account/dashboard'), api.get<FundingStatus>('/sandbox/funding/status'),
      ])
      setData(dashboard.data); setFunding(status.data)
    } catch (caught) { setError(problemMessage(caught, 'Não foi possível carregar dados atuais.')); setData(null) }
  }, [])
  useEffect(() => { void load() }, [load])

  const fund = async () => {
    setFundingBusy(true); setError('')
    try { await api.post('/sandbox/funding'); await load() }
    catch (caught) { setError(problemMessage(caught)) }
    finally { setFundingBusy(false) }
  }

  if (!data && !error) return <Loading label="Consultando saldo atual…" />
  return (
    <div className="page-stack">
      <header className="page-heading"><div><span className="eyebrow">Conta de demonstração</span><h1>Olá, {user?.fullName.split(' ')[0]}</h1><p>Visão atualizada do seu ambiente fictício.</p></div><span className="status-pill"><i /> Ambiente operacional</span></header>
      <ErrorMessage message={error} />
      {data ? <>
        <section className="balance-card">
          <div><span>Saldo fictício disponível</span><button type="button" className="icon-button" onClick={() => setHidden((value) => !value)} aria-label={hidden ? 'Mostrar saldo' : 'Ocultar saldo'}>{hidden ? '◉' : '◌'}</button></div>
          <strong>{hidden ? 'R$ ••••••' : money.format(data.account.balance)}</strong>
          <p>Agência {data.account.agency} · Conta {data.account.accountNumber}-{data.account.accountDigit}</p>
          <div className="balance-meta"><span>Limite diário <b>{money.format(data.account.dailyLimit)}</b></span><span>Usado hoje <b>{money.format(data.account.transferredToday)}</b></span></div>
        </section>
        <section className="quick-actions" aria-label="Ações rápidas">
          <Link to="/app/transfer"><span>↗</span><strong>Transferir</strong><small>chave interna</small></Link>
          <Link to="/app/statement"><span>≡</span><strong>Extrato</strong><small>ledger completo</small></Link>
          <Link to="/app/keys"><span>⌘</span><strong>Minhas chaves</strong><small>{data.keys.length} ativas</small></Link>
          <button type="button" onClick={() => void fund()} disabled={!funding?.available || fundingBusy}><span>＋</span><strong>Recarregar</strong><small>{funding?.available ? `até ${money.format(funding.amountAvailable)}` : 'indisponível agora'}</small></button>
        </section>
        <div className="dashboard-grid">
          <section className="panel"><div className="panel-heading"><div><h2>Movimentações recentes</h2><p>Dados confirmados pelo backend.</p></div><Link to="/app/statement">Ver extrato →</Link></div>
            {data.recentEntries.length ? <div className="transaction-list">{data.recentEntries.map((entry) => <article key={entry.id}><span className={`transaction-icon transaction-icon--${entry.type.toLowerCase()}`}>{entry.type === 'CREDIT' ? '↓' : '↑'}</span><div><strong>{entry.description}</strong><small>{formatDate(entry.createdAt)} · {entry.category.replaceAll('_', ' ')}</small></div><b className={entry.type === 'CREDIT' ? 'positive' : ''}>{entry.type === 'CREDIT' ? '+' : '−'} {money.format(entry.amount)}</b></article>)}</div> : <Empty title="Sem movimentações" text="Seus lançamentos aparecerão aqui." />}
          </section>
          <aside className="panel security-panel"><div className="panel-heading"><div><h2>Segurança</h2><p>Status da sua conta.</p></div></div><div className="security-score"><strong>{user?.pinConfigured ? '100' : '70'}%</strong><span>Proteção configurada</span></div><ul><li className="done">Senha segura</li><li className="done">Sessão protegida</li><li className={user?.pinConfigured ? 'done' : 'pending'}>{user?.pinConfigured ? 'PIN configurado' : 'Crie seu PIN'}</li></ul>{!user?.pinConfigured ? <Link className="button button--ghost button--full" to="/app/profile">Configurar PIN</Link> : null}</aside>
        </div>
      </> : <button className="button button--primary" onClick={() => void load()}>Tentar novamente</button>}
    </div>
  )
}
