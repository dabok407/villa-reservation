import { useState } from 'react'
import type { Reservation } from '../../types/reservation'
import { parseDate, formatDateFull, getNights } from '../../utils/dateUtils'
import { verifyPassword, updateReservation, cancelReservation } from '../../api/reservationApi'

interface Props {
  reservation: Reservation
  onClose: () => void
  onUpdated: () => void
}

type Mode = 'detail' | 'password' | 'edit'

export default function ReservationDetailModal({ reservation, onClose, onUpdated }: Props) {
  const [mode, setMode] = useState<Mode>('detail')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Edit form state
  const [editCheckIn, setEditCheckIn] = useState(reservation.checkInDate)
  const [editCheckOut, setEditCheckOut] = useState(reservation.checkOutDate)
  const [editAdult, setEditAdult] = useState(reservation.adultCount)
  const [editChild, setEditChild] = useState(reservation.childCount)
  const [editDesc, setEditDesc] = useState(reservation.description || '')

  function formatEditDate(dateStr: string): string {
    const [y, m, d] = dateStr.split('-')
    return `${y}. ${m}. ${d}`
  }

  const checkIn = parseDate(reservation.checkInDate)
  const checkOut = parseDate(reservation.checkOutDate)
  const nights = getNights(checkIn, checkOut)
  const peopleStr = [
    reservation.adultCount > 0 ? `성인 ${reservation.adultCount}` : '',
    reservation.childCount > 0 ? `아이 ${reservation.childCount}` : '',
  ].filter(Boolean).join(', ')

  async function handleVerifyPassword() {
    setLoading(true)
    setError('')
    try {
      const valid = await verifyPassword(reservation.id, password)
      if (valid) {
        setMode('edit')
      } else {
        setError('비밀번호가 일치하지 않습니다')
      }
    } catch {
      setError('확인에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  async function handleUpdate() {
    setLoading(true)
    setError('')
    try {
      await updateReservation(reservation.id, {
        password,
        checkInDate: editCheckIn,
        checkOutDate: editCheckOut,
        adultCount: editAdult,
        childCount: editChild,
        description: editDesc || undefined,
      })
      onUpdated()
    } catch (err) {
      setError(err instanceof Error ? err.message : '수정에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  async function handleCancel() {
    if (!confirm('정말 예약을 취소하시겠습니까?')) return
    setLoading(true)
    try {
      await cancelReservation(reservation.id, password)
      onUpdated()
    } catch (err) {
      setError(err instanceof Error ? err.message : '취소에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ background: 'rgba(0,0,0,0.4)' }} onClick={onClose}>
      <div className="glass rounded-3xl max-w-sm w-full p-5 space-y-4 overlay shadow-2xl max-h-[90vh] overflow-y-auto overflow-x-hidden" onClick={e => e.stopPropagation()}>

        {/* Detail View */}
        {mode === 'detail' && (
          <>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-villa-500/10 rounded-lg flex items-center justify-center">
                  <i className="ri-calendar-line text-villa-500"></i>
                </div>
                <h3 className="font-bold text-gray-800">예약 상세</h3>
              </div>
              <button onClick={onClose} className="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center transition">
                <i className="ri-close-line text-gray-400 text-lg"></i>
              </button>
            </div>

            <div className="space-y-3">
              <div className="flex items-center gap-3 bg-gray-50 rounded-xl p-3">
                <i className="ri-user-line text-villa-500"></i>
                <div>
                  <p className="text-xs text-gray-500">예약자</p>
                  <p className="font-semibold text-gray-800">{reservation.reserverName}</p>
                </div>
              </div>
              <div className="flex items-start gap-3 bg-gray-50 rounded-xl p-3">
                <i className="ri-calendar-2-line text-villa-500 mt-0.5"></i>
                <div className="min-w-0">
                  <p className="text-xs text-gray-500">기간</p>
                  <p className="font-semibold text-gray-800 text-sm break-keep">
                    {formatDateFull(checkIn)} ~ {formatDateFull(checkOut)} · {nights}박
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3 bg-gray-50 rounded-xl p-3">
                <i className="ri-group-line text-villa-500"></i>
                <div>
                  <p className="text-xs text-gray-500">인원</p>
                  <p className="font-semibold text-gray-800">{peopleStr}</p>
                </div>
              </div>
              {reservation.description && (
                <div className="flex items-center gap-3 bg-gray-50 rounded-xl p-3">
                  <i className="ri-chat-3-line text-villa-500"></i>
                  <div>
                    <p className="text-xs text-gray-500">내용</p>
                    <p className="font-semibold text-gray-800">{reservation.description}</p>
                  </div>
                </div>
              )}
            </div>

            <div className="flex gap-2 pt-2">
              <button onClick={onClose} className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold py-3 rounded-xl transition text-sm">
                닫기
              </button>
              <button onClick={() => setMode('password')} className="flex-1 btn-primary text-white font-semibold py-3 rounded-xl text-sm flex items-center justify-center gap-1">
                <i className="ri-edit-line"></i> 예약 변경
              </button>
            </div>
          </>
        )}

        {/* Password View */}
        {mode === 'password' && (
          <>
            <div className="text-center">
              <div className="w-14 h-14 bg-villa-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
                <i className="ri-lock-line text-2xl text-villa-500"></i>
              </div>
              <h3 className="font-bold text-gray-800">비밀번호 확인</h3>
              <p className="text-sm text-gray-500 mt-1">예약 시 설정한 비밀번호를 입력하세요</p>
            </div>
            <input
              type="text"
              inputMode="numeric"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleVerifyPassword()}
              placeholder="비밀번호"
              autoComplete="off"
              style={{ WebkitTextSecurity: 'disc' } as React.CSSProperties}
              className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm text-center focus:border-villa-500 focus:ring-2 focus:ring-villa-100 outline-none transition"
            />
            {error && <p className="text-red-500 text-xs text-center">{error}</p>}
            <div className="flex gap-2">
              <button onClick={() => { setMode('detail'); setError('') }} className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold py-3 rounded-xl transition text-sm">
                취소
              </button>
              <button onClick={handleVerifyPassword} disabled={loading} className="flex-1 btn-primary text-white font-semibold py-3 rounded-xl text-sm disabled:opacity-50">
                {loading ? '확인중...' : '확인'}
              </button>
            </div>
          </>
        )}

        {/* Edit View */}
        {mode === 'edit' && (
          <form noValidate onSubmit={e => { e.preventDefault(); handleUpdate(); }} className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-gray-800">예약 수정</h3>
              <button type="button" onClick={onClose} className="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center transition">
                <i className="ri-close-line text-gray-400 text-lg"></i>
              </button>
            </div>

            <div className="bg-gray-50 rounded-2xl p-4 space-y-3">
              <div>
                <label className="text-sm font-semibold text-gray-700 block mb-1.5">입실일</label>
                <div className="relative">
                  <div className="bg-white border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 pointer-events-none">
                    {formatEditDate(editCheckIn)}
                  </div>
                  <input type="date" value={editCheckIn} onChange={e => setEditCheckIn(e.target.value)}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                </div>
              </div>
              <div>
                <label className="text-sm font-semibold text-gray-700 block mb-1.5">퇴실일</label>
                <div className="relative">
                  <div className="bg-white border-2 border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 pointer-events-none">
                    {formatEditDate(editCheckOut)}
                  </div>
                  <input type="date" value={editCheckOut} onChange={e => setEditCheckOut(e.target.value)}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3">
              <div className="bg-gray-50 rounded-2xl p-4 flex items-center justify-between">
                <p className="font-semibold text-gray-800 text-sm">성인</p>
                <div className="flex items-center gap-3">
                  <button type="button" className="counter-btn" onClick={() => setEditAdult(c => Math.max(0, c - 1))}>−</button>
                  <span className="text-lg font-bold text-gray-800 w-6 text-center">{editAdult}</span>
                  <button type="button" className="counter-btn" onClick={() => setEditAdult(c => c + 1)}>+</button>
                </div>
              </div>
              <div className="bg-gray-50 rounded-2xl p-4 flex items-center justify-between">
                <p className="font-semibold text-gray-800 text-sm">아이</p>
                <div className="flex items-center gap-3">
                  <button type="button" className="counter-btn" onClick={() => setEditChild(c => Math.max(0, c - 1))}>−</button>
                  <span className="text-lg font-bold text-gray-800 w-6 text-center">{editChild}</span>
                  <button type="button" className="counter-btn" onClick={() => setEditChild(c => c + 1)}>+</button>
                </div>
              </div>
            </div>

            <div>
              <label className="text-sm font-semibold text-gray-700 mb-2 block">내용</label>
              <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} rows={2}
                className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-villa-500 outline-none transition resize-none" />
            </div>

            {error && <p className="text-red-500 text-xs text-center">{error}</p>}

            <div className="flex gap-2">
              <button type="button" onClick={handleCancel} disabled={loading}
                className="bg-red-50 hover:bg-red-100 text-red-600 font-semibold py-3 px-4 rounded-xl transition text-sm">
                예약 취소
              </button>
              <button type="submit" disabled={loading}
                className="flex-1 btn-primary text-white font-semibold py-3 rounded-xl text-sm disabled:opacity-50">
                {loading ? '처리중...' : '수정 완료'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
