import { useState, useEffect, useCallback } from 'react'
import type { Reservation, CheckoutMemo, DateSelection } from './types/reservation'
import { fetchReservations, fetchLatestCheckoutMemo, fetchActiveReservationsToday } from './api/reservationApi'
import Header from './components/Header'
import Calendar from './components/Calendar'
import ReservationForm from './components/ReservationForm'
import ReservationDetailModal from './components/modals/ReservationDetailModal'
import CheckoutModal from './components/modals/CheckoutModal'

export default function App() {
  const [currentYear, setCurrentYear] = useState(new Date().getFullYear())
  const [currentMonth, setCurrentMonth] = useState(new Date().getMonth())
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [latestMemo, setLatestMemo] = useState<CheckoutMemo | null>(null)
  const [dateSelection, setDateSelection] = useState<DateSelection>({ checkIn: null, checkOut: null })
  const [selectedReservation, setSelectedReservation] = useState<Reservation | null>(null)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [showCheckoutModal, setShowCheckoutModal] = useState(false)
  const [activeToday, setActiveToday] = useState<Reservation[]>([])

  const loadData = useCallback(async () => {
    try {
      const [res, memo, today] = await Promise.all([
        fetchReservations(currentYear, currentMonth + 1),
        fetchLatestCheckoutMemo(),
        fetchActiveReservationsToday(),
      ])
      setReservations(res)
      setLatestMemo(memo)
      setActiveToday(today)
    } catch {
      // silently fail on load
    }
  }, [currentYear, currentMonth])

  useEffect(() => {
    loadData()
  }, [loadData])

  function goToPrevMonth() {
    if (currentMonth === 0) {
      setCurrentMonth(11)
      setCurrentYear(y => y - 1)
    } else {
      setCurrentMonth(m => m - 1)
    }
    resetSelection()
  }

  function goToNextMonth() {
    if (currentMonth === 11) {
      setCurrentMonth(0)
      setCurrentYear(y => y + 1)
    } else {
      setCurrentMonth(m => m + 1)
    }
    resetSelection()
  }

  function resetSelection() {
    setDateSelection({ checkIn: null, checkOut: null })
  }

  function handleReservationComplete() {
    resetSelection()
    loadData()
  }

  function handleDayClick(date: Date, reservation?: Reservation) {
    if (reservation) {
      setSelectedReservation(reservation)
      setShowDetailModal(true)
      return
    }

    if (!dateSelection.checkIn || dateSelection.checkOut) {
      setDateSelection({ checkIn: date, checkOut: null })
    } else {
      if (date <= dateSelection.checkIn) {
        setDateSelection({ checkIn: date, checkOut: null })
      } else {
        // Check if any reservation falls between checkIn and checkout
        const checkIn = dateSelection.checkIn!
        const hasConflict = reservations.some(r => {
          // UTC 파싱 방지: "2026-04-17" → local midnight로 변환
          const [iy, im, id] = r.checkInDate.split('-').map(Number)
          const [oy, om, od] = r.checkOutDate.split('-').map(Number)
          const rIn = new Date(iy, im - 1, id)
          const rOut = new Date(oy, om - 1, od)
          // 예약 구간이 [checkIn, date] 범위와 겹치는지 확인
          return rIn < date && rOut > checkIn
        })
        if (hasConflict) {
          setDateSelection({ checkIn: date, checkOut: null })
        } else {
          setDateSelection({ checkIn: dateSelection.checkIn, checkOut: date })
        }
      }
    }
  }

  function handleCheckoutClick() {
    if (activeToday.length === 0) {
      alert('오늘 체크아웃 가능한 예약이 없습니다')
      return
    }
    if (activeToday.length === 1) {
      setSelectedReservation(activeToday[0])
    } else {
      setSelectedReservation(null)
    }
    setShowCheckoutModal(true)
  }

  return (
    <div className="bg-gradient-to-br from-slate-50 via-white to-violet-50 min-h-screen">
      <Header onCheckout={handleCheckoutClick} />

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <Calendar
          year={currentYear}
          month={currentMonth}
          reservations={reservations}
          dateSelection={dateSelection}
          onDayClick={handleDayClick}
          onPrevMonth={goToPrevMonth}
          onNextMonth={goToNextMonth}
        />

        {dateSelection.checkIn && dateSelection.checkOut && (
          <ReservationForm
            checkIn={dateSelection.checkIn}
            checkOut={dateSelection.checkOut}
            latestMemo={latestMemo}
            onComplete={handleReservationComplete}
            onCancel={resetSelection}
          />
        )}
      </main>

      {showDetailModal && selectedReservation && (
        <ReservationDetailModal
          reservation={selectedReservation}
          onClose={() => setShowDetailModal(false)}
          onUpdated={() => { setShowDetailModal(false); loadData() }}
        />
      )}

      {showCheckoutModal && (
        <CheckoutModal
          reservations={activeToday}
          preSelected={selectedReservation}
          onClose={() => setShowCheckoutModal(false)}
          onComplete={() => { setShowCheckoutModal(false); loadData() }}
        />
      )}
    </div>
  )
}
