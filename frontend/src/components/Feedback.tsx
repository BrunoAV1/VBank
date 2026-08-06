export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return <div className="state-card" role="status"><span className="spinner" aria-hidden="true" />{label}</div>
}

export function Empty({ title, text }: { title: string; text: string }) {
  return <div className="state-card state-card--empty"><strong>{title}</strong><span>{text}</span></div>
}

export function ErrorMessage({ message }: { message?: string }) {
  return message ? <div className="form-error" role="alert">{message}</div> : null
}

