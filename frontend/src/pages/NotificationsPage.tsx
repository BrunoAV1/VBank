import { useCallback, useEffect, useState } from 'react'
import { api, problemMessage } from '../api/client'
import type { Notification, Page } from '../api/types'
import { Empty, ErrorMessage, Loading } from '../components/Feedback'
import { formatDate } from '../utils/format'

export default function NotificationsPage() {
  const [page, setPage] = useState<Page<Notification> | null>(null)
  const [number, setNumber] = useState(0)
  const [error, setError] = useState('')
  const load = useCallback(async () => { try { const { data } = await api.get<Page<Notification>>('/notifications', { params: { page: number, size: 20, sort: 'createdAt,desc' } }); setPage(data) } catch (caught) { setError(problemMessage(caught)) } }, [number])
  useEffect(() => { void load() }, [load])
  const read = async (id: string) => { await api.patch(`/notifications/${id}/read`); await load() }
  const readAll = async () => { await api.patch('/notifications/read-all'); await load() }
  return <div className="page-stack"><header className="page-heading"><div><span className="eyebrow">Caixa interna</span><h1>Notificações</h1><p>Alertas gerados dentro da plataforma, sem e-mail ou SMS.</p></div><button className="button button--ghost" onClick={() => void readAll()}>Marcar todas como lidas</button></header><ErrorMessage message={error} />{!page ? <Loading /> : page.content.length === 0 ? <Empty title="Tudo tranquilo" text="Nenhuma notificação por enquanto." /> : <><section className="panel notification-list">{page.content.map((item) => <button key={item.id} className={item.read ? 'read' : ''} onClick={() => void read(item.id)}><span className="notification-icon">{item.type.includes('TRANSFER') ? '↗' : item.type.includes('FUNDING') ? '＋' : '◇'}</span><span><strong>{item.title}</strong><p>{item.message}</p><small>{formatDate(item.createdAt)}</small></span>{!item.read ? <i aria-label="Não lida" /> : null}</button>)}</section><div className="pagination"><button disabled={page.first} onClick={() => setNumber((value) => value - 1)}>← Anterior</button><span>Página {page.number + 1} de {Math.max(page.totalPages, 1)}</span><button disabled={page.last} onClick={() => setNumber((value) => value + 1)}>Próxima →</button></div></>}</div>
}
