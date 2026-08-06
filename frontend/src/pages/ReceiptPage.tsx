import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, problemMessage } from '../api/client'
import type { Transfer } from '../api/types'
import { ErrorMessage, Loading } from '../components/Feedback'
import { formatDate, money } from '../utils/format'

export default function ReceiptPage() {
  const { id } = useParams()
  const [transfer, setTransfer] = useState<Transfer | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api.get<Transfer>(`/pix/transfers/${id}/receipt`).then(({ data }) => setTransfer(data)).catch((caught) => setError(problemMessage(caught))) }, [id])
  const download = async () => {
    try {
      const response = await api.get<Blob>(`/pix/transfers/${id}/receipt.pdf`, { responseType: 'blob' })
      const url = URL.createObjectURL(response.data); const anchor = document.createElement('a'); anchor.href = url; anchor.download = `comprovante-${transfer?.publicId}.pdf`; anchor.click(); URL.revokeObjectURL(url)
    } catch (caught) { setError(problemMessage(caught, 'Não foi possível baixar o PDF.')) }
  }
  if (!transfer && !error) return <Loading label="Gerando comprovante…" />
  return <div className="page-stack receipt-page"><header className="page-heading no-print"><div><span className="eyebrow">Documento sandbox</span><h1>Comprovante</h1><p>Registro de uma transferência sem valor financeiro real.</p></div><div className="button-row"><button className="button button--ghost" onClick={() => window.print()}>Imprimir</button><button className="button button--primary" onClick={() => void download()}>Baixar PDF</button></div></header><ErrorMessage message={error} />{transfer ? <article className="receipt"><header><span className="brand"><span className="brand-mark">V</span>VBank Sandbox</span><span className="status-pill"><i /> {transfer.status}</span></header><div className="receipt-value"><small>Valor fictício transferido</small><strong>{money.format(transfer.amount)}</strong><span>Transferência sandbox concluída</span></div><dl><div><dt>Pagador</dt><dd>{transfer.payerName}</dd></div><div><dt>Destinatário</dt><dd>{transfer.recipientName}</dd></div><div><dt>Data e hora</dt><dd>{formatDate(transfer.completedAt)}</dd></div><div><dt>Chave interna utilizada</dt><dd>{transfer.keyUsed}</dd></div><div><dt>Descrição</dt><dd>{transfer.description || 'Sem descrição'}</dd></div><div><dt>ID da transferência</dt><dd><code>{transfer.publicId}</code></dd></div><div><dt>End-to-End ID fictício</dt><dd><code>{transfer.endToEndId}</code></dd></div></dl><div className="receipt-warning"><strong>Comprovante sem valor financeiro</strong><p>Este documento pertence a um ambiente educacional. Não comprova pagamento, Pix ou movimentação bancária real.</p></div><footer>VBank Sandbox · Projeto de código aberto sob licença MIT</footer></article> : null}<Link className="text-link no-print" to="/app/statement">← Voltar ao extrato</Link></div>
}
