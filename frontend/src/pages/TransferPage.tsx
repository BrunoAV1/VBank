import { zodResolver } from '@hookform/resolvers/zod'
import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { z } from 'zod'
import { api, problemMessage } from '../api/client'
import type { Transfer } from '../api/types'
import { ErrorMessage } from '../components/Feedback'
import { money } from '../utils/format'

const schema = z.object({
  key: z.string().min(1, 'Informe a chave interna.'),
  amount: z.number().min(0.01, 'O valor mínimo é R$ 0,01.'),
  description: z.string().max(140).optional(),
  pin: z.string().regex(/^\d{6}$/, 'O PIN deve ter 6 dígitos.'),
})
type FormData = z.infer<typeof schema>
interface Resolved { maskedName: string; type: string; keyDisplay: string; accountStatus: string }

export default function TransferPage() {
  const [step, setStep] = useState(1)
  const [recipient, setRecipient] = useState<Resolved | null>(null)
  const [result, setResult] = useState<Transfer | null>(null)
  const [error, setError] = useState('')
  const idempotencyKey = useRef(crypto.randomUUID())
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { key: '', amount: 0, description: '', pin: '' } })
  const values = form.watch()

  const resolve = async () => {
    setError('')
    const key = form.getValues('key')
    if (!key.trim()) { form.setError('key', { message: 'Informe a chave interna.' }); return }
    try { const { data } = await api.post<Resolved>('/pix/resolve', { key }); setRecipient(data); setStep(2) }
    catch (caught) { setError(problemMessage(caught)) }
  }
  const submit = async (data: FormData) => {
    setError('')
    try {
      const response = await api.post<Transfer>('/pix/transfers', data, { headers: { 'Idempotency-Key': idempotencyKey.current } })
      form.setValue('pin', ''); setResult(response.data); setStep(5)
    } catch (caught) { form.setValue('pin', ''); setError(problemMessage(caught, 'Nenhuma transferência foi realizada.')) }
  }
  const restart = () => { form.reset(); setRecipient(null); setResult(null); setStep(1); setError(''); idempotencyKey.current = crypto.randomUUID() }

  return (
    <div className="page-stack transfer-page">
      <header className="page-heading"><div><span className="eyebrow">Transferência sandbox</span><h1>Transferir saldo fictício</h1><p>O backend valida PIN, saldo, limite, chave e idempotência.</p></div></header>
      <ol className="stepper" aria-label="Etapas da transferência">{['Chave', 'Valor', 'Revisão', 'PIN', 'Resultado'].map((label, index) => <li key={label} className={step >= index + 1 ? 'active' : ''}><span>{index + 1}</span>{label}</li>)}</ol>
      <ErrorMessage message={error} />
      <section className="transfer-card">
        {step === 1 ? <div className="transfer-step"><span className="step-icon">⌘</span><h2>Para quem você vai transferir?</h2><p>Informe uma chave cadastrada dentro do VBank Sandbox.</p><label>Chave Pix simulada<input autoFocus {...form.register('key')} placeholder="e-mail, telefone, @username ou UUID" aria-invalid={!!form.formState.errors.key} /><small className="field-error">{form.formState.errors.key?.message}</small></label><button className="button button--primary button--full" type="button" onClick={() => void resolve()}>Localizar destinatário</button></div> : null}
        {step === 2 && recipient ? <div className="transfer-step"><div className="recipient-card"><span className="avatar">{recipient.maskedName[0]}</span><div><small>Destinatário localizado</small><strong>{recipient.maskedName}</strong><span>{recipient.type} · uso interno</span></div><b>✓</b></div><h2>Qual é o valor fictício?</h2><label>Valor (R$)<input autoFocus type="number" min="0.01" step="0.01" {...form.register('amount', { valueAsNumber: true })} aria-invalid={!!form.formState.errors.amount} /><small className="field-error">{form.formState.errors.amount?.message}</small></label><label>Descrição opcional<textarea rows={3} maxLength={140} {...form.register('description')} /></label><div className="button-row"><button className="button button--ghost" onClick={() => setStep(1)}>Voltar</button><button className="button button--primary" onClick={async () => { const valid = await form.trigger(['amount', 'description']); if (valid) setStep(3) }}>Revisar</button></div></div> : null}
        {step === 3 && recipient ? <div className="transfer-step"><span className="eyebrow">Confira com atenção</span><h2>Revisão da transferência</h2><dl className="review-list"><div><dt>Destinatário</dt><dd>{recipient.maskedName}</dd></div><div><dt>Chave interna</dt><dd>{recipient.keyDisplay}</dd></div><div><dt>Valor fictício</dt><dd className="review-amount">{money.format(Number(values.amount))}</dd></div><div><dt>Descrição</dt><dd>{values.description || 'Sem descrição'}</dd></div></dl><div className="warning-box">Esta operação não usa Pix real e não movimenta dinheiro.</div><div className="button-row"><button className="button button--ghost" onClick={() => setStep(2)}>Editar</button><button className="button button--primary" onClick={() => setStep(4)}>Confirmar e informar PIN</button></div></div> : null}
        {step === 4 ? <form className="transfer-step" onSubmit={form.handleSubmit(submit)}><span className="step-icon">◇</span><h2>Confirme com seu PIN</h2><p>Digite os 6 dígitos. Após cinco erros, o PIN será bloqueado temporariamente.</p><label>PIN de segurança<input autoFocus type="password" inputMode="numeric" autoComplete="off" maxLength={6} {...form.register('pin')} aria-invalid={!!form.formState.errors.pin} /><small className="field-error">{form.formState.errors.pin?.message}</small></label><button className="button button--primary button--full" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? 'Processando com segurança…' : `Transferir ${money.format(Number(values.amount))} fictícios`}</button><button className="text-button" type="button" onClick={() => setStep(3)}>Voltar à revisão</button></form> : null}
        {step === 5 && result ? <div className="transfer-step transfer-success"><span className="success-mark">✓</span><span className="eyebrow">Transferência concluída</span><h2>{money.format(result.amount)}</h2><p>Valor fictício enviado para {result.recipientName}.</p><dl className="receipt-mini"><div><dt>ID</dt><dd>{result.publicId}</dd></div><div><dt>End-to-End fictício</dt><dd>{result.endToEndId}</dd></div></dl><div className="button-row"><Link className="button button--primary" to={`/app/receipt/${result.id}`}>Ver comprovante</Link><button className="button button--ghost" onClick={restart}>Nova transferência</button></div></div> : null}
      </section>
    </div>
  )
}
