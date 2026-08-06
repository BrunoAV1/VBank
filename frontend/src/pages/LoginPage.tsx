import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { problemMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/Feedback'
import { SandboxNotice } from '../components/SandboxNotice'
import { useBackendStatus } from '../status/BackendStatusContext'

const schema = z.object({ identifier: z.string().min(1, 'Informe seu e-mail ou username.'), password: z.string().min(1, 'Informe sua senha.') })
type FormData = z.infer<typeof schema>

export default function LoginPage() {
  const { user, login } = useAuth()
  const { state, retry } = useBackendStatus()
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const location = useLocation()
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) })
  if (user) return <Navigate to="/app/dashboard" replace />

  const submit = async (data: FormData) => {
    setError('')
    try {
      await login(data.identifier, data.password)
      const target = (location.state as { from?: string } | null)?.from || '/app/dashboard'
      navigate(target, { replace: true })
    } catch (caught) { setError(problemMessage(caught, 'Não foi possível entrar.')) }
  }

  return (
    <div className="auth-page">
      <section className="auth-brand-panel"><Link className="brand" to="/"><span className="brand-mark">V</span>VBank <em>Sandbox</em></Link><div><span className="eyebrow">Conta de demonstração</span><h1>Operações fictícias.<br />Aprendizado real.</h1><p>Entre para consultar seu saldo fictício, suas chaves internas e seus comprovantes sandbox.</p></div><SandboxNotice compact /></section>
      <main className="auth-form-panel" id="main-content">
        <form className="auth-form" onSubmit={handleSubmit(submit)} noValidate>
          <span className="eyebrow">Acesso seguro</span><h2>Boas-vindas de volta</h2><p>Use o e-mail ou username da sua conta de demonstração.</p>
          {state === 'down' ? <div className="warning-box" role="alert">O ambiente está indisponível. Nenhuma operação será enviada. <button type="button" onClick={() => void retry()}>Tentar novamente</button></div> : null}
          <ErrorMessage message={error} />
          <label>E-mail ou username<input autoComplete="username" autoFocus {...register('identifier')} aria-invalid={!!errors.identifier} /><small className="field-error">{errors.identifier?.message}</small></label>
          <label>Senha<input type="password" autoComplete="current-password" {...register('password')} aria-invalid={!!errors.password} /><small className="field-error">{errors.password?.message}</small></label>
          <button className="button button--primary button--full" disabled={isSubmitting || state !== 'up'}>{isSubmitting ? 'Entrando…' : 'Entrar'}</button>
          <p className="form-switch">Ainda não tem uma conta? <Link to="/register">Criar conta de demonstração</Link></p>
          <small className="form-help">Não há recuperação por e-mail. Este projeto não envia mensagens ou SMS.</small>
        </form>
      </main>
    </div>
  )
}
