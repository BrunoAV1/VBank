import { Link } from 'react-router'
import { SandboxNotice } from '../components/SandboxNotice'

const features = [
  ['↗', 'Transferências sandbox', 'Envie saldo fictício com chave interna, PIN e comprovante auditável.'],
  ['≡', 'Ledger consistente', 'Cada alteração de saldo gera lançamentos de débito e crédito no PostgreSQL.'],
  ['⌘', 'Chaves internas', 'E-mail, telefone, username e chave aleatória — nunca conectados ao Pix real.'],
  ['◇', 'Segurança educacional', 'JWT curto, refresh rotativo HttpOnly, BCrypt, idempotência e locks no banco.'],
]

export default function LandingPage() {
  return (
    <div className="public-page">
      <header className="public-header">
        <Link className="brand" to="/"><span className="brand-mark">V</span><span>VBank <em>Sandbox</em></span></Link>
        <nav aria-label="Acesso"><a href="#recursos">Recursos</a><a href="#tecnologia">Tecnologia</a><Link to="/login">Entrar</Link><Link className="button button--small button--primary" to="/register">Criar conta</Link></nav>
      </header>
      <main id="main-content">
        <section className="hero">
          <div className="hero-copy">
            <span className="eyebrow"><i className="live-dot" /> Código aberto · MIT · custo zero</span>
            <h1>Aprenda operações bancárias.<br /><span>Sem movimentar dinheiro real.</span></h1>
            <p>Um ambiente completo para estudar autenticação, ledger, concorrência e transferências transacionais com Java, React e PostgreSQL.</p>
            <div className="button-row"><Link className="button button--primary" to="/register">Criar conta de demonstração <span>→</span></Link><Link className="button button--ghost" to="/login">Já tenho uma conta</Link></div>
            <ul className="hero-checks"><li>R$ 50.000,00 fictícios ao cadastrar</li><li>Sem cartão e sem serviços financeiros externos</li><li>Pronto para GitHub e Vercel</li></ul>
          </div>
          <div className="hero-visual" aria-label="Prévia ilustrativa do dashboard">
            <div className="mock-window">
              <div className="mock-top"><span /><span /><span /></div>
              <div className="mock-balance"><small>Saldo fictício disponível</small><strong>R$ 50.000,00</strong><span>Conta de demonstração ativa</span></div>
              <div className="mock-actions"><span>↗<small>Transferir</small></span><span>⌘<small>Chaves</small></span><span>≡<small>Extrato</small></span></div>
              <div className="mock-line"><i>↓</i><span><strong>Saldo inicial fictício</strong><small>Hoje, 09:41</small></span><b>+ R$ 50.000,00</b></div>
            </div>
            <div className="floating-chip floating-chip--one">✓ Transação atômica</div>
            <div className="floating-chip floating-chip--two">◇ Nenhum dinheiro real</div>
          </div>
        </section>
        <SandboxNotice />
        <section className="section" id="recursos">
          <span className="eyebrow">Dentro do sandbox</span><h2>Um banco fictício, com engenharia de verdade.</h2>
          <div className="feature-grid">{features.map(([icon, title, text]) => <article className="feature-card" key={title}><span>{icon}</span><h3>{title}</h3><p>{text}</p></article>)}</div>
        </section>
        <section className="tech-section" id="tecnologia">
          <div><span className="eyebrow">Arquitetura aberta</span><h2>React e Spring Boot no mesmo deploy</h2><p>O frontend compilado é servido pela API, que persiste exclusivamente no PostgreSQL Neon. O Dockerfile remoto empacota tudo sem exigir Docker local.</p><a href="https://github.com/BrunoAV1/vbank-sandbox" target="_blank" rel="noreferrer">Ver projeto no GitHub →</a></div>
          <div className="architecture-line"><span>React + TypeScript</span><i>→</i><span>Spring Boot</span><i>→</i><span>PostgreSQL</span></div>
        </section>
      </main>
      <footer className="public-footer"><span>VBank Sandbox © 2026 · Licença MIT</span><SandboxNotice compact /></footer>
    </div>
  )
}
