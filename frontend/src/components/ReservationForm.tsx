import { useState } from 'react'
import type { CheckoutMemo } from '../types/reservation'
import { RESERVER_NAMES } from '../types/reservation'
import { formatDateWithDay, getNights, toISODate } from '../utils/dateUtils'
import { createReservation } from '../api/reservationApi'
import ConfirmModal from './modals/ConfirmModal'

interface ReservationFormProps {
  checkIn: Date
  checkOut: Date
  latestMemo: CheckoutMemo | null
  onComplete: () => void
  onCancel: () => void
}

export default function ReservationForm({ checkIn, checkOut, latestMemo, onComplete, onCancel }: ReservationFormProps) {
  const [reserverName, setReserverName] = useState('')
  const [customName, setCustomName] = useState('')
  const [adultCount, setAdultCount] = useState(1)
  const [childCount, setChildCount] = useState(0)
  const [password, setPassword] = useState('')
  const [description, setDescription] = useState('')
  const [showConfirm, setShowConfirm] = useState(false)
  const [loading, setLoading] = useState(false)

  const nights = getNights(checkIn, checkOut)
  const actualName = reserverName === '기타' ? customName : reserverName

  function handleSubmit() {
    if (!actualName.trim()) {
      alert('예약자를 선택하세요')
      return
    }
    if (adultCount + childCount === 0) {
      alert('인원을 입력하세요')
      return
    }
    if (!password) {
      alert('비밀번호를 입력하세요')
      return
    }
    setShowConfirm(true)
  }

  async function handleConfirm() {
    setLoading(true)
    try {
      await createReservation({
        reserverName: actualName.trim(),
        checkInDate: toISODate(checkIn),
        checkOutDate: toISODate(checkOut),
        adultCount,
        childCount,
        password,
        description: description || undefined,
      })
      onComplete()
    } catch (err) {
      alert(err instanceof Error ? err.message : '예약에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="glass rounded-3xl shadow-xl shadow-violet-100/50 border border-white/60 p-6 space-y-5 fade-in">
        <div className="flex items-center gap-2 mb-1">
          <div className="w-8 h-8 bg-villa-500/10 rounded-lg flex items-center justify-center">
            <i className="ri-edit-line text-villa-500"></i>
          </div>
          <h3 className="font-bold text-gray-800">예약 정보 입력</h3>
          <button onClick={onCancel} className="ml-auto text-gray-400 hover:text-gray-600 text-sm">
            <i className="ri-close-line text-lg"></i>
          </button>
        </div>

        {/* 선택된 기간 */}
        <div className="bg-gradient-to-r from-indigo-50 to-purple-50 rounded-2xl p-4 flex items-center justify-between">
          <div className="text-center">
            <p className="text-xs text-gray-500 mb-1">입실</p>
            <p className="text-lg font-bold text-villa-500">{formatDateWithDay(checkIn)}</p>
          </div>
          <div className="flex items-center gap-2 text-gray-400">
            <div className="w-8 h-[2px] bg-gray-300"></div>
            <span className="text-sm font-semibold bg-villa-500 text-white rounded-full w-8 h-8 flex items-center justify-center">
              {nights}박
            </span>
            <div className="w-8 h-[2px] bg-gray-300"></div>
          </div>
          <div className="text-center">
            <p className="text-xs text-gray-500 mb-1">퇴실</p>
            <p className="text-lg font-bold text-villa-800">{formatDateWithDay(checkOut)}</p>
          </div>
        </div>

        {/* 예약자 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-2 block">
            예약자 <span className="text-red-400">*</span>
          </label>
          <div className="relative">
            <select
              value={reserverName}
              onChange={e => setReserverName(e.target.value)}
              className="w-full appearance-none bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-villa-500 focus:ring-2 focus:ring-villa-100 outline-none transition"
            >
              <option value="">예약자를 선택하세요</option>
              {RESERVER_NAMES.map(name => (
                <option key={name} value={name}>
                  {name === '기타' ? '기타 (직접입력)' : name}
                </option>
              ))}
            </select>
            <i className="ri-arrow-down-s-line absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"></i>
          </div>
          {reserverName === '기타' && (
            <input
              type="text"
              value={customName}
              onChange={e => setCustomName(e.target.value)}
              placeholder="이름을 입력하세요"
              className="w-full mt-2 bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-villa-500 focus:ring-2 focus:ring-villa-100 outline-none transition"
            />
          )}
        </div>

        {/* 인원 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-3 block">
            인원 <span className="text-red-400">*</span>
          </label>
          <div className="grid grid-cols-1 gap-3">
            <div className="bg-gray-50 rounded-2xl p-4 flex items-center justify-between">
              <div>
                <p className="font-semibold text-gray-800 text-sm">성인</p>
                <p className="text-xs text-gray-400">만 13세 이상</p>
              </div>
              <div className="flex items-center gap-3">
                <button className="counter-btn" onClick={() => setAdultCount(c => Math.max(0, c - 1))}>−</button>
                <span className="text-lg font-bold text-gray-800 w-6 text-center">{adultCount}</span>
                <button className="counter-btn" onClick={() => setAdultCount(c => c + 1)}>+</button>
              </div>
            </div>
            <div className="bg-gray-50 rounded-2xl p-4 flex items-center justify-between">
              <div>
                <p className="font-semibold text-gray-800 text-sm">아이</p>
                <p className="text-xs text-gray-400">만 12세 이하</p>
              </div>
              <div className="flex items-center gap-3">
                <button className="counter-btn" onClick={() => setChildCount(c => Math.max(0, c - 1))}>−</button>
                <span className="text-lg font-bold text-gray-800 w-6 text-center">{childCount}</span>
                <button className="counter-btn" onClick={() => setChildCount(c => c + 1)}>+</button>
              </div>
            </div>
          </div>
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-2 block">
            비밀번호 <span className="text-red-400">*</span>
          </label>
          <input
            type="text"
            inputMode="numeric"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="예약 수정/삭제 시 필요합니다"
            autoComplete="off"
            style={{ WebkitTextSecurity: 'disc' } as React.CSSProperties}
            className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-villa-500 focus:ring-2 focus:ring-villa-100 outline-none transition"
          />
        </div>

        {/* 내용 */}
        <div>
          <label className="text-sm font-semibold text-gray-700 mb-2 block">
            내용 <span className="text-gray-400 font-normal">(선택)</span>
          </label>
          <textarea
            value={description}
            onChange={e => setDescription(e.target.value)}
            rows={3}
            placeholder="예) 대학친구들 부부동반 모임"
            className="w-full bg-white border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:border-villa-500 focus:ring-2 focus:ring-villa-100 outline-none transition resize-none"
          />
        </div>

        {/* 예약 버튼 */}
        <button
          onClick={handleSubmit}
          className="btn-primary w-full text-white font-semibold py-4 rounded-2xl text-base flex items-center justify-center gap-2"
        >
          <i className="ri-calendar-check-line text-lg"></i>
          예약하기
        </button>
      </div>

      {showConfirm && (
        <ConfirmModal
          reserverName={actualName}
          checkIn={checkIn}
          checkOut={checkOut}
          adultCount={adultCount}
          childCount={childCount}
          description={description}
          latestMemo={latestMemo}
          loading={loading}
          onConfirm={handleConfirm}
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </>
  )
}
