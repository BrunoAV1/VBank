export type Role = 'USER' | 'ADMIN' | 'SYSTEM'
export type UserStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED'
export type AccountStatus = 'ACTIVE' | 'TEMPORARILY_BLOCKED' | 'CLOSED' | 'SYSTEM'
export type PixKeyType = 'EMAIL' | 'PHONE' | 'USERNAME' | 'RANDOM'

export interface User {
  id: string
  fullName: string
  email: string
  username: string
  status: UserStatus
  roles: Role[]
  pinConfigured: boolean
  createdAt: string
}

export interface TokenResponse { accessToken: string; tokenType: string; expiresInSeconds: number; user: User }
export interface Account {
  id: string; agency: string; accountNumber: string; accountDigit: string
  balance: number; dailyLimit: number; transferredToday: number; status: AccountStatus; createdAt: string
}
export interface PixKey { id: string; type: PixKeyType; displayValue: string; status: string; createdAt: string }
export interface LedgerEntry {
  id: string; type: 'CREDIT' | 'DEBIT'; category: string; amount: number
  resultingBalance: number; description: string; transferId?: string; createdAt: string
}
export interface Transfer {
  id: string; publicId: string; endToEndId: string; amount: number; description?: string
  status: string; payerName: string; recipientName: string; keyUsed: string
  createdAt: string; completedAt: string; fictitious: boolean
}
export interface Notification { id: string; title: string; message: string; type: string; read: boolean; createdAt: string }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; first: boolean; last: boolean }
export interface DashboardData { account: Account; recentEntries: LedgerEntry[]; keys: PixKey[]; unreadNotifications: number }
export interface FundingStatus { available: boolean; currentBalance: number; amountAvailable: number; nextAvailableAt?: string }
export interface Health { status: 'UP' | 'DOWN'; application: string; database: string; version: string; timestamp: string }
export interface Problem { title?: string; detail?: string; code?: string; errors?: Record<string, string> }

