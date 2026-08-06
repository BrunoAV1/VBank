import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import DashboardPage from '../pages/DashboardPage'
import TransferPage from '../pages/TransferPage'
import ReceiptPage from '../pages/ReceiptPage'
import { Unavailable } from '../components/Unavailable'

const mocks = vi.hoisted(() => ({
  get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  login: vi.fn(), register: vi.fn(), retry: vi.fn(),
  authUser: null as null | { id: string; fullName: string; email: string; username: string; status: 'ACTIVE'; roles: ('USER' | 'ADMIN')[]; pinConfigured: boolean; createdAt: string },
  backendState: 'up' as 'up' | 'down' | 'checking',
}))

vi.mock('../api/client', () => ({
  api: { get: mocks.get, post: mocks.post, patch: mocks.patch, delete: mocks.delete },
  problemMessage: (_error: unknown, fallback = 'Falha') => fallback,
}))
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: mocks.authUser, loading: false, login: mocks.login, register: mocks.register,
    logout: vi.fn(), logoutAll: vi.fn(), reloadUser: vi.fn() }),
}))
vi.mock('../status/BackendStatusContext', () => ({
  useBackendStatus: () => ({ state: mocks.backendState, health: null, retry: mocks.retry }),
}))

beforeEach(() => {
  vi.clearAllMocks(); mocks.authUser = null; mocks.backendState = 'up'
  mocks.login.mockResolvedValue(undefined); mocks.register.mockResolvedValue(undefined)
})

describe('autenticação e validação', () => {
  it('valida login e envia credenciais sem armazená-las', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><LoginPage /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('Informe seu e-mail ou username.')).toBeVisible()
    await user.type(screen.getByRole('textbox', { name: /E-mail ou username/ }), 'alice')
    await user.type(document.querySelector('input[name="password"]') as HTMLInputElement, 'SenhaForte123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await waitFor(() => expect(mocks.login).toHaveBeenCalledWith('alice', 'SenhaForte123'))
  })

  it('não aceita cadastro sem os termos fictícios', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><RegisterPage /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /Criar conta e receber/ }))
    expect(await screen.findByText('Aceite os termos do ambiente fictício.')).toBeVisible()
    expect(mocks.register).not.toHaveBeenCalled()
  })
})

it('mostra dashboard apenas com dados atuais do backend', async () => {
  mocks.authUser = { id: 'u1', fullName: 'Alice Sandbox', email: 'alice@example.test', username: 'alice', status: 'ACTIVE', roles: ['USER'], pinConfigured: true, createdAt: '2026-01-01T00:00:00Z' }
  mocks.get.mockImplementation((url: string) => {
    if (url === '/account/dashboard') return Promise.resolve({ data: { account: { id: 'a1', agency: '0001', accountNumber: '123', accountDigit: '4', balance: 50000, dailyLimit: 10000, transferredToday: 0, status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' }, recentEntries: [], keys: [], unreadNotifications: 0 } })
    return Promise.resolve({ data: { available: false, currentBalance: 50000, amountAvailable: 0 } })
  })
  render(<MemoryRouter><DashboardPage /></MemoryRouter>)
  expect(await screen.findByText('R$ 50.000,00')).toBeVisible()
  expect(screen.getByText('Dados confirmados pelo backend.')).toBeVisible()
})

it('executa as etapas da transferência e mostra o resultado', async () => {
  const user = userEvent.setup()
  mocks.post.mockImplementation((url: string) => {
    if (url === '/pix/resolve') return Promise.resolve({ data: { maskedName: 'B***o S*****x', type: 'EMAIL', keyDisplay: 'bruno@example.test', accountStatus: 'ACTIVE' } })
    if (url === '/pix/transfers') return Promise.resolve({ data: { id: 't1', publicId: 'TRX-1', endToEndId: 'E2E-SANDBOX-1', amount: 1000, status: 'COMPLETED', payerName: 'Alice', recipientName: 'Bruno', keyUsed: 'bruno@example.test', createdAt: '2026-01-01T00:00:00Z', completedAt: '2026-01-01T00:00:00Z', fictitious: true } })
    return Promise.reject(new Error('URL inesperada'))
  })
  render(<MemoryRouter><TransferPage /></MemoryRouter>)
  await user.type(screen.getByLabelText('Chave Pix simulada'), 'bruno@example.test')
  await user.click(screen.getByRole('button', { name: 'Localizar destinatário' }))
  await screen.findByText('B***o S*****x')
  await user.clear(screen.getByLabelText('Valor (R$)')); await user.type(screen.getByLabelText('Valor (R$)'), '1000')
  await user.click(screen.getByRole('button', { name: 'Revisar' }))
  await user.click(screen.getByRole('button', { name: 'Confirmar e informar PIN' }))
  await user.type(screen.getByLabelText('PIN de segurança'), '123456')
  await user.click(screen.getByRole('button', { name: /Transferir.*1\.000,00 fictícios/ }))
  expect(await screen.findByText('Transferência concluída')).toBeVisible()
  expect(screen.getByText('E2E-SANDBOX-1')).toBeVisible()
})

it('trata indisponibilidade sem indicar sucesso', async () => {
  mocks.backendState = 'down'
  const user = userEvent.setup()
  render(<Unavailable />)
  expect(screen.getByText(/Nenhuma transferência foi realizada/)).toBeVisible()
  await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
  expect(mocks.retry).toHaveBeenCalled()
})

it('renderiza comprovante fictício e seu identificador', async () => {
  mocks.get.mockResolvedValue({ data: { id: 't1', publicId: 'TRX-ABC', endToEndId: 'E2E-SANDBOX-ABC', amount: 1000, description: 'Teste', status: 'COMPLETED', payerName: 'Alice', recipientName: 'Bruno', keyUsed: '@bruno', createdAt: '2026-01-01T12:00:00Z', completedAt: '2026-01-01T12:00:00Z', fictitious: true } })
  render(<MemoryRouter initialEntries={['/app/receipt/t1']}><Routes><Route path="/app/receipt/:id" element={<ReceiptPage />} /></Routes></MemoryRouter>)
  expect(await screen.findByText('TRX-ABC')).toBeVisible()
  expect(screen.getByText('Comprovante sem valor financeiro')).toBeVisible()
})
