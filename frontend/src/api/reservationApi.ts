import type {
  Reservation,
  ReservationRequest,
  ReservationUpdateRequest,
  CheckoutRequest,
  CheckoutMemo,
} from '../types/reservation'

const BASE = '/villa/api'

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || `요청 실패 (${res.status})`)
  }
  if (res.status === 204) return null as T
  const text = await res.text()
  if (!text) return null as T
  return JSON.parse(text)
}

export async function fetchReservations(year: number, month: number): Promise<Reservation[]> {
  const res = await fetch(`${BASE}/reservations?year=${year}&month=${month}`)
  return handleResponse(res)
}

export async function fetchReservation(id: number): Promise<Reservation> {
  const res = await fetch(`${BASE}/reservations/${id}`)
  return handleResponse(res)
}

export async function createReservation(req: ReservationRequest): Promise<Reservation> {
  const res = await fetch(`${BASE}/reservations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return handleResponse(res)
}

export async function verifyPassword(id: number, password: string): Promise<boolean> {
  const res = await fetch(`${BASE}/reservations/${id}/verify-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
  const data = await handleResponse<{ valid: boolean }>(res)
  return data.valid
}

export async function updateReservation(id: number, req: ReservationUpdateRequest): Promise<Reservation> {
  const res = await fetch(`${BASE}/reservations/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return handleResponse(res)
}

export async function checkout(id: number, req: CheckoutRequest): Promise<void> {
  const res = await fetch(`${BASE}/reservations/${id}/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return handleResponse(res)
}

export async function cancelReservation(id: number, password: string): Promise<void> {
  const res = await fetch(`${BASE}/reservations/${id}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
  return handleResponse(res)
}

export async function fetchLatestCheckoutMemo(): Promise<CheckoutMemo | null> {
  const res = await fetch(`${BASE}/checkout-memos/latest`)
  if (res.status === 204) return null
  return handleResponse(res)
}

export async function fetchActiveReservationsToday(): Promise<Reservation[]> {
  const res = await fetch(`${BASE}/reservations/active-today`)
  return handleResponse(res)
}
