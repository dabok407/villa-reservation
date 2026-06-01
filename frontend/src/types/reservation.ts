export interface Reservation {
  id: number
  reserverName: string
  checkInDate: string
  checkOutDate: string
  adultCount: number
  childCount: number
  description: string | null
  status: 'ACTIVE' | 'CHECKED_OUT' | 'CANCELLED'
  createdAt: string
}

export interface ReservationRequest {
  reserverName: string
  checkInDate: string
  checkOutDate: string
  adultCount: number
  childCount: number
  password: string
  description?: string
}

export interface ReservationUpdateRequest {
  password: string
  checkInDate: string
  checkOutDate: string
  adultCount: number
  childCount: number
  description?: string
}

export interface CheckoutRequest {
  password: string
  memo?: string
}

export interface CheckoutMemo {
  id: number
  reserverName: string
  memo: string
  checkoutDate: string
}

export type DateSelection = {
  checkIn: Date | null
  checkOut: Date | null
}

export const RESERVER_NAMES = [
  '김경임', '황용귀', '박정인', '황대한', '배지현', '황민국', '기타'
] as const
