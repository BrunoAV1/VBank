import { Link } from 'react-router'
import { SandboxNotice } from '../components/SandboxNotice'

export default function NotFoundPage() {
  return <main className="centered-page" id="main-content"><section className="unavailable-card"><span className="eyebrow">Erro 404</span><h1>Página não encontrada</h1><p>Esta rota não existe no ambiente de demonstração.</p><Link className="button button--primary" to="/">Voltar ao início</Link><SandboxNotice compact /></section></main>
}
