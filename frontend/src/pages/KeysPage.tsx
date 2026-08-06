import { useCallback, useEffect, useState } from 'react'
import { api, problemMessage } from '../api/client'
import type { PixKey, PixKeyType } from '../api/types'
import { Empty, ErrorMessage, Loading } from '../components/Feedback'
import { formatDate } from '../utils/format'

const labels: Record<PixKeyType, string> = { EMAIL: 'E-mail', PHONE: 'Telefone interno', USERNAME: 'Username', RANDOM: 'Aleatória' }

export default function KeysPage() {
  const [keys, setKeys] = useState<PixKey[] | null>(null)
  const [type, setType] = useState<PixKeyType>('RANDOM')
  const [value, setValue] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const load = useCallback(async () => { try { const { data } = await api.get<PixKey[]>('/pix/keys'); setKeys(data) } catch (caught) { setError(problemMessage(caught)) } }, [])
  useEffect(() => { void load() }, [load])
  const create = async (event: React.FormEvent) => {
    event.preventDefault(); setBusy(true); setError('')
    try { await api.post('/pix/keys', { type, value: type === 'RANDOM' ? null : value }); setValue(''); await load() }
    catch (caught) { setError(problemMessage(caught)) } finally { setBusy(false) }
  }
  const remove = async (id: string) => {
    if (!window.confirm('Excluir esta chave interna? Ela deixará de localizar sua conta de demonstração.')) return
    try { await api.delete(`/pix/keys/${id}`); await load() } catch (caught) { setError(problemMessage(caught)) }
  }
  const copy = async (text: string) => { await navigator.clipboard.writeText(text) }
  return (
    <div className="page-stack">
      <header className="page-heading"><div><span className="eyebrow">Identificadores internos</span><h1>Chaves Pix simuladas</h1><p>Funcionam somente entre contas desta plataforma.</p></div></header>
      <div className="warning-box">Nenhuma chave desta tela é registrada no Pix real. Não informe CPF ou CNPJ.</div>
      <ErrorMessage message={error} />
      <div className="two-column">
        <section className="panel"><div className="panel-heading"><div><h2>Minhas chaves</h2><p>{keys?.length ?? 0} chaves ativas</p></div></div>{keys === null ? <Loading /> : keys.length === 0 ? <Empty title="Nenhuma chave cadastrada" text="Crie uma chave interna para receber transferências sandbox." /> : <div className="key-list">{keys.map((key) => <article key={key.id}><span className="key-icon">⌘</span><div><strong>{labels[key.type]}</strong><code>{key.displayValue}</code><small>Criada em {formatDate(key.createdAt)}</small></div><button className="icon-button" onClick={() => void copy(key.displayValue)} aria-label={`Copiar ${key.displayValue}`}>▣</button><button className="icon-button danger" onClick={() => void remove(key.id)} aria-label={`Excluir ${key.displayValue}`}>×</button></article>)}</div>}</section>
        <form className="panel form-panel" onSubmit={create}><div className="panel-heading"><div><h2>Nova chave interna</h2><p>Escolha um identificador da conta.</p></div></div><label>Tipo<select value={type} onChange={(event) => setType(event.target.value as PixKeyType)}>{Object.entries(labels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>{type !== 'RANDOM' ? <label>Valor<input required value={value} onChange={(event) => setValue(event.target.value)} placeholder={type === 'EMAIL' ? 'Mesmo e-mail do cadastro' : type === 'USERNAME' ? '@seuusername' : '+55 11 99999-9999'} /></label> : <p className="hint-box">Uma chave UUID aleatória será gerada pelo backend.</p>}<button className="button button--primary button--full" disabled={busy}>{busy ? 'Criando…' : 'Criar chave simulada'}</button></form>
      </div>
    </div>
  )
}

