import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, problemMessage } from '../api/client'
import type { LedgerEntry, Page } from '../api/types'
import { Empty, ErrorMessage, Loading } from '../components/Feedback'
import { formatDate, money } from '../utils/format'

export default function StatementPage() {
  const [page, setPage] = useState<Page<LedgerEntry> | null>(null)
  const [number, setNumber] = useState(0)
  const [type, setType] = useState('')
  const [search, setSearch] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setError('')
    try { const { data } = await api.get<Page<LedgerEntry>>('/account/statement', { params: {
      page: number, size: 12, type: type || undefined, search: search || undefined,
      from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
      to: to ? endExclusive(to) : undefined,
      minAmount: minAmount || undefined, maxAmount: maxAmount || undefined, sort: 'createdAt,desc',
    } }); setPage(data) }
    catch (caught) { setError(problemMessage(caught)); setPage(null) }
  }, [number, type, search, from, to, minAmount, maxAmount])
  useEffect(() => { void load() }, [load])
  return (
    <div className="page-stack">
      <header className="page-heading"><div><span className="eyebrow">Ledger da conta</span><h1>Extrato</h1><p>Cada alteração de saldo é registrada pelo backend.</p></div></header>
      <section className="filter-bar filter-bar--wrap"><label>Buscar<input value={search} onChange={(event) => { setSearch(event.target.value); setNumber(0) }} placeholder="Descrição" /></label><label>Movimento<select value={type} onChange={(event) => { setType(event.target.value); setNumber(0) }}><option value="">Todos</option><option value="CREDIT">Entradas</option><option value="DEBIT">Saídas</option></select></label><label>De<input type="date" value={from} onChange={(event) => { setFrom(event.target.value); setNumber(0) }} /></label><label>Até<input type="date" value={to} onChange={(event) => { setTo(event.target.value); setNumber(0) }} /></label><label>Valor mínimo<input type="number" min="0" step="0.01" value={minAmount} onChange={(event) => { setMinAmount(event.target.value); setNumber(0) }} /></label><label>Valor máximo<input type="number" min="0" step="0.01" value={maxAmount} onChange={(event) => { setMaxAmount(event.target.value); setNumber(0) }} /></label><button className="button button--ghost" onClick={() => { setSearch(''); setType(''); setFrom(''); setTo(''); setMinAmount(''); setMaxAmount(''); setNumber(0) }}>Limpar</button></section>
      <ErrorMessage message={error} />
      {!page && !error ? <Loading label="Carregando extrato…" /> : null}
      {page ? <section className="panel"><div className="statement-table" role="table" aria-label="Lançamentos do extrato"><div className="table-head" role="row"><span>Movimentação</span><span>Data</span><span>Saldo após</span><span>Valor</span></div>{page.content.map((entry) => <div className="table-row" role="row" key={entry.id}><span><i className={`transaction-icon transaction-icon--${entry.type.toLowerCase()}`}>{entry.type === 'CREDIT' ? '↓' : '↑'}</i><span><strong>{entry.description}</strong><small>{entry.category.replaceAll('_', ' ')}</small></span></span><span>{formatDate(entry.createdAt)}</span><span>{money.format(entry.resultingBalance)}</span><span className={entry.type === 'CREDIT' ? 'positive' : ''}>{entry.type === 'CREDIT' ? '+' : '−'} {money.format(entry.amount)}{entry.transferId ? <Link aria-label="Ver comprovante" to={`/app/receipt/${entry.transferId}`}>Comprovante</Link> : null}</span></div>)}</div>{page.content.length === 0 ? <Empty title="Nenhum lançamento encontrado" text="Altere os filtros ou faça uma operação sandbox." /> : null}<div className="pagination"><button disabled={page.first} onClick={() => setNumber((value) => value - 1)}>← Anterior</button><span>Página {page.number + 1} de {Math.max(page.totalPages, 1)}</span><button disabled={page.last} onClick={() => setNumber((value) => value + 1)}>Próxima →</button></div></section> : null}
    </div>
  )
}

function endExclusive(date: string) {
  const value = new Date(`${date}T00:00:00`)
  value.setDate(value.getDate() + 1)
  return value.toISOString()
}
