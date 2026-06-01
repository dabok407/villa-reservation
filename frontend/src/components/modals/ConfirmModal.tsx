import type { CheckoutMemo } from '../../types/reservation'
import { formatDateWithDay, getNights } from '../../utils/dateUtils'

interface ConfirmModalProps {
  reserverName: string
  checkIn: Date
  checkOut: Date
  adultCount: number
  childCount: number
  description: string
  latestMemo: CheckoutMemo | null
  loading: boolean
  onConfirm: () => void
  onCancel: () => void
}

export default function ConfirmModal({
  reserverName, checkIn, checkOut, adultCount, childCount, description,
  latestMemo, loading, onConfirm, onCancel,
}: ConfirmModalProps) {
  const nights = getNights(checkIn, checkOut)
  const peopleStr = [
    adultCount > 0 ? `성인 ${adultCount}` : '',
    childCount > 0 ? `아이 ${childCount}` : '',
  ].filter(Boolean).join(', ')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.4)' }} onClick={onCancel}>
      <div className="glass rounded-3xl max-w-sm w-full p-6 space-y-4 overlay shadow-2xl" onClick={e => e.stopPropagation()}>
        <div className="text-center">
          <div className="w-16 h-16 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <i className="ri-calendar-check-line text-3xl text-villa-500"></i>
          </div>
          <h3 className="font-bold text-gray-800 text-lg">예약 확인</h3>
          <p className="text-sm text-gray-500 mt-1">아래 내용으로 예약하시겠습니까?</p>
        </div>

        <div className="bg-gradient-to-r from-indigo-50 to-purple-50 rounded-2xl p-4 space-y-2.5">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">예약자</span>
            <span className="font-semibold text-gray-800">{reserverName}</span>
          </div>
          <div className="flex justify-between text-sm gap-2">
            <span className="text-gray-500 shrink-0">기간</span>
            <span className="font-semibold text-gray-800 text-right">
              {formatDateWithDay(checkIn)} ~ {formatDateWithDay(checkOut)} · {nights}박
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">인원</span>
            <span className="font-semibold text-gray-800">{peopleStr}</span>
          </div>
          {description && (
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">내용</span>
              <span className="font-semibold text-gray-800">{description}</span>
            </div>
          )}
        </div>

        {latestMemo && (
          <div className="checkout-memo-card rounded-xl p-3">
            <p className="text-xs font-semibold text-amber-800 mb-1">
              이전 이용자 메모 ({latestMemo.reserverName} · {latestMemo.checkoutDate.slice(5).replace('-', '.')} 체크아웃)
            </p>
            <p className="text-xs text-amber-700 leading-relaxed">{latestMemo.memo}</p>
          </div>
        )}

        <div className="flex gap-2 pt-1">
          <button
            onClick={onCancel}
            className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold py-3.5 rounded-xl transition text-sm"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 btn-primary text-white font-semibold py-3.5 rounded-xl text-sm disabled:opacity-50"
          >
            {loading ? '처리중...' : '확인'}
          </button>
        </div>
      </div>
    </div>
  )
}
