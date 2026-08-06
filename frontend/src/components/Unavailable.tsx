import { useBackendStatus } from '../status/BackendStatusContext'

export function Unavailable() {
  const { state, retry } = useBackendStatus()
  return (
    <main className="centered-page" id="main-content">
      <section className="unavailable-card" aria-live="polite">
        <div className="status-orb" aria-hidden="true" />
        <span className="eyebrow">Conexão segura indisponível</span>
        <h1>O ambiente está iniciando</h1>
        <p>O ambiente bancário de demonstração está iniciando ou temporariamente indisponível. Nenhuma transferência foi realizada.</p>
        <button className="button button--primary" type="button" onClick={() => void retry()} disabled={state === 'checking'}>
          {state === 'checking' ? 'Verificando…' : 'Tentar novamente'}
        </button>
        <small>Planos gratuitos podem suspender recursos depois de um período sem uso.</small>
      </section>
    </main>
  )
}

