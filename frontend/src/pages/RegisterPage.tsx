import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { z } from 'zod'
import { problemMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/Feedback'
import { SandboxNotice } from '../components/SandboxNotice'
import { useBackendStatus } from '../status/BackendStatusContext'

const schema = z.object({
  fullName: z.string().min(3, 'Informe seu nome completo.').max(160),
  email: z.email('Informe um e-mail válido.'),
  username: z.string().regex(/^[A-Za-z0-9._-]{3,40}$/, 'Use 3 a 40 letras, números, ponto, hífen ou sublinhado.'),
  password: z.string().min(8, 'Use ao menos 8 caracteres.').max(72).regex(/[A-Z]/, 'Inclua uma letra maiúscula.').regex(/[a-z]/, 'Inclua uma letra minúscula.').regex(/\d/, 'Inclua um número.'),
  passwordConfirmation: z.string(),
  acceptedTerms: z.boolean().refine(Boolean, 'Aceite os termos do ambiente fictício.'),
}).refine((data) => data.password === data.passwordConfirmation, { path: ['passwordConfirmation'], message: 'As senhas não coincidem.' })
type FormData = z.infer<typeof schema>

export default function RegisterPage() {
  const { user, register: createAccount } = useAuth()
  const { state, retry } = useBackendStatus()
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { acceptedTerms: false } })
  if (user) return <Navigate to="/app/dashboard" replace />
  const submit = async (data: FormData) => {
    setError('')
    try { await createAccount(data); navigate('/app/profile', { replace: true, state: { setupPin: true } }) }
    catch (caught) { setError(problemMessage(caught, 'Não foi possível criar a conta.')) }
  }
  return (
    <div className="auth-page auth-page--register">
      <section className="auth-brand-panel"><Link className="brand" to="/"><span className="brand-mark">V</span>VBank <em>Sandbox</em></Link><div><span className="eyebrow">Comece gratuitamente</span><h1>R$ 50 mil fictícios.<br />Zero risco financeiro.</h1><ul className="benefit-list"><li>Saldo inicial e ledger automáticos</li><li>Chaves internas, extrato e PDF</li><li>Sem CPF, cartão ou conta bancária real</li></ul></div><SandboxNotice compact /></section>
      <main className="auth-form-panel" id="main-content">
        <form className="auth-form auth-form--wide" onSubmit={form.handleSubmit(submit)} noValidate>
          <span className="eyebrow">Nova conta</span><h2>Crie sua conta de demonstração</h2><p>Use somente dados adequados a um ambiente de aprendizado.</p>
          {state === 'down' ? <div className="warning-box" role="alert">O ambiente está indisponível. <button type="button" onClick={() => void retry()}>Tentar novamente</button></div> : null}
          <ErrorMessage message={error} />
          <div className="form-grid">
            <label className="span-2">Nome completo<input autoComplete="name" autoFocus {...form.register('fullName')} aria-invalid={!!form.formState.errors.fullName} /><small className="field-error">{form.formState.errors.fullName?.message}</small></label>
            <label>E-mail<input type="email" autoComplete="email" {...form.register('email')} aria-invalid={!!form.formState.errors.email} /><small className="field-error">{form.formState.errors.email?.message}</small></label>
            <label>Username<div className="input-prefix"><span>@</span><input autoComplete="username" {...form.register('username')} aria-invalid={!!form.formState.errors.username} /></div><small className="field-error">{form.formState.errors.username?.message}</small></label>
            <label>Senha<input type="password" autoComplete="new-password" {...form.register('password')} aria-invalid={!!form.formState.errors.password} /><small className="field-error">{form.formState.errors.password?.message}</small></label>
            <label>Confirmar senha<input type="password" autoComplete="new-password" {...form.register('passwordConfirmation')} aria-invalid={!!form.formState.errors.passwordConfirmation} /><small className="field-error">{form.formState.errors.passwordConfirmation?.message}</small></label>
          </div>
          <label className="check-label"><input type="checkbox" {...form.register('acceptedTerms')} /><span>Entendo que esta é uma conta de demonstração, que o saldo é fictício e que nenhuma transferência possui valor financeiro real.</span></label><small className="field-error">{form.formState.errors.acceptedTerms?.message}</small>
          <button className="button button--primary button--full" disabled={form.formState.isSubmitting || state !== 'up'}>{form.formState.isSubmitting ? 'Criando conta…' : 'Criar conta e receber saldo fictício'}</button>
          <p className="form-switch">Já tem uma conta? <Link to="/login">Entrar</Link></p>
        </form>
      </main>
    </div>
  )
}
