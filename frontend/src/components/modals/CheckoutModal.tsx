import { useState } from 'react'
import type { Reservation } from '../../types/reservation'
import { checkout } from '../../api/reservationApi'

interface Props {
  reservations: Reservation[]
  preSelected: Reservation | null
  onClose: () => void
  onComplete: () => void
}

export default function CheckoutModal({ reservations, preSelected, onClose, onComplete }: Props) {
  const [selected, setSelected] = useState<Reservation | null>(preSelected || (reservations.length === 1 ? reservations[0] : null))
  const [password, setPassword] = useState('')
  const [memo, setMemo] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleCheckout() {
    if (!selected) {
      setError('예약을 선택하세요')
      return
    }
    if (!password) {
      setError('비밀번호를 입력하세요')
      return
    }
    setLoading(true)
    setError('')
    try {
      await checkout(selected.id, { password, memo: memo || undefined })
      onComplete()
    } catch (err) {
      setError(err instanceof Error ? err.message : '체크아웃에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.4)' }} onClick={onClose}>
      <div className="glass rounded-3xl max-w-sm w-full p-6 space-y-4 overlay shadow-2xl" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-pink-500/10 rounded-lg flex items-center justify-center">
              <i className="ri-logout-box-line text-pink-500"></i>
            </div>
            <h3 className="font-bold text-gray-800">체크아웃</h3>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center transition">
            <i className="ri-close-line text-gray-400 text-lg"></i>
          </button>
        </div>

        {/* 예약 선택 (여러 건일 경우) */}
        {reservations.length > 1 && (
          <div>
            <label className="text-sm font-semibold text-gray-700 mb-2 block">예약 선택</label>
            {reservations.map(r => (
              <button
                key={r.id}
                onClick={() => setSelected(r)}
                className={`w-full text-left p-3 rounded-xl mb-2 text-sm transition ${
                  selected?.id === r.id ? 'bg-pink-50 border-2 border-pink-300' : 'bg-gray-50 border-2 border-transparent'
                }`}
              >
                <p className="font-semibold">{r.reserverName}</p>
                <p className="text-xs text-gray-500">{r.checkInDate} ~ {r.checkOutDate}</p>
              </button>
            ))}
          </div>
        )}

        {/* 현재 예약 정보 */}
        {selected && (
          <div className="bg-pink-50 rounded-xl p-3 text-sm">
            <p className="text-pink-800"><span className="font-semibold">{selected.reserverName}</span>님의 예약</p>
            <p className="text-pink-600 text-xs mt-1">
              {selected.checkInDate} ~ {selected.checkOutDate}
            </p>
          </div>
        )}

        {/* 비밀번호 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-2 block">비밀번호 확인</label>
          <input
            type="text"
            inputMode="numeric"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="예약 시 설정한 비밀번호"
            autoComplete="off"
            style={{ WebkitTextSecurity: 'disc' } as React.CSSProperties}
            className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-pink-400 focus:ring-2 focus:ring-pink-100 outline-none transition"
          />
        </div>

        {/* 메모 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-2 block">다음 이용자에게 남기는 메모</label>
          <textarea
            value={memo}
            onChange={e => setMemo(e.target.value)}
            rows={4}
            placeholder="예) 소주 3병 남아있음, 쌀 없음 - 사와야 함, 보일러 온도 25도로 설정해둠"
            className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-pink-400 focus:ring-2 focus:ring-pink-100 outline-none transition resize-none"
          />
        </div>

        {error && <p className="text-red-500 text-xs text-center">{error}</p>}

        <button
          type="button"
          onClick={handleCheckout}
          disabled={loading}
          className="btn-checkout w-full text-white font-semibold py-3.5 rounded-2xl text-sm flex items-center justify-center gap-2 disabled:opacity-50"
        >
          <i className="ri-logout-box-line"></i>
          {loading ? '처리중...' : '체크아웃 완료'}
        </button>
      </div>
    </div>
  )
}
