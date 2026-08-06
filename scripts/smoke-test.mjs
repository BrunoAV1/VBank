const baseUrl = (process.argv[2] || 'http://localhost:8080').replace(/\/$/, '')
const unique = Date.now().toString(36)

async function call(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } })
  if (!response.ok) throw new Error(`${options.method || 'GET'} ${path}: HTTP ${response.status} ${await response.text()}`)
  if (response.status === 204) return null
  return response.headers.get('content-type')?.includes('json') ? response.json() : response.arrayBuffer()
}

function auth(token) { return { Authorization: `Bearer ${token}` } }
const password = 'SmokeSenha123'
const users = [
  { fullName: 'Smoke Origem', email: `origem-${unique}@example.test`, username: `origem-${unique}` },
  { fullName: 'Smoke Destino', email: `destino-${unique}@example.test`, username: `destino-${unique}` },
]

console.log('1/8 health')
const health = await call('/api/health'); if (health.status !== 'UP') throw new Error('Health não está UP')
console.log('2/8 cadastro de dois usuários')
for (const user of users) Object.assign(user, await call('/api/auth/register', { method: 'POST', body: JSON.stringify({ ...user, password, passwordConfirmation: password, acceptedTerms: true }) }))
for (const user of users) await call('/api/me/pin', { method: 'POST', headers: auth(user.accessToken), body: JSON.stringify({ pin: '123456' }) })
console.log('3/8 chave interna')
await call('/api/pix/keys', { method: 'POST', headers: auth(users[1].accessToken), body: JSON.stringify({ type: 'EMAIL', value: users[1].email }) })
console.log('4/8 resolução')
await call('/api/pix/resolve', { method: 'POST', headers: auth(users[0].accessToken), body: JSON.stringify({ key: users[1].email }) })
console.log('5/8 transferência')
const transfer = await call('/api/pix/transfers', { method: 'POST', headers: { ...auth(users[0].accessToken), 'Idempotency-Key': `smoke-${unique}` }, body: JSON.stringify({ key: users[1].email, amount: 1000, description: 'Smoke test', pin: '123456' }) })
console.log('6/8 saldos')
const [originBalance, destinationBalance] = await Promise.all(users.map((user) => call('/api/account/balance', { headers: auth(user.accessToken) })))
if (Number(originBalance.balance) !== 49000 || Number(destinationBalance.balance) !== 51000) throw new Error('Saldos finais incorretos')
console.log('7/8 extratos')
const statements = await Promise.all(users.map((user) => call('/api/account/statement?size=10', { headers: auth(user.accessToken) })))
if (statements.some((page) => page.content.length < 2)) throw new Error('Extrato incompleto')
console.log('8/8 comprovante JSON e PDF')
await call(`/api/pix/transfers/${transfer.id}/receipt`, { headers: auth(users[0].accessToken) })
const pdf = await call(`/api/pix/transfers/${transfer.id}/receipt.pdf`, { headers: auth(users[0].accessToken) })
if (!new TextDecoder().decode(pdf.slice(0, 4)).startsWith('%PDF')) throw new Error('PDF inválido')
console.log('Smoke test concluído: R$ 49.000,00 e R$ 51.000,00 fictícios.')

