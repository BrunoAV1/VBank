export function SandboxNotice({ compact = false }: { compact?: boolean }) {
  return (
    <aside className={compact ? 'sandbox-notice sandbox-notice--compact' : 'sandbox-notice'} aria-label="Aviso importante de ambiente fictício">
      <span aria-hidden="true">◇</span>
      <p><strong>Ambiente bancário fictício.</strong> Para demonstração, aprendizado e testes. Nenhum valor ou transferência realizada nesta plataforma possui valor financeiro real.</p>
    </aside>
  )
}

