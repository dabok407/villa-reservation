import type { Reservation, DateSelection } from '../types/reservation'
import { getDaysInMonth, getFirstDayOfMonth, isSameDay, parseDate } from '../utils/dateUtils'

interface CalendarProps {
  year: number
  month: number
  reservations: Reservation[]
  dateSelection: DateSelection
  onDayClick: (date: Date, reservation?: Reservation) => void
  onPrevMonth: () => void
  onNextMonth: () => void
}

const DAY_HEADERS = ['일', '월', '화', '수', '목', '금', '토']

export default function Calendar({
  year, month, reservations, dateSelection, onDayClick, onPrevMonth, onNextMonth,
}: CalendarProps) {
  const daysInMonth = getDaysInMonth(year, month)
  const firstDay = getFirstDayOfMonth(year, month)
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  function getReservationForDate(date: Date): Reservation | undefined {
    return reservations.find(r => {
      const checkIn = parseDate(r.checkInDate)
      const checkOut = parseDate(r.checkOutDate)
      return date >= checkIn && date < checkOut
    })
  }

  function getDayClass(date: Date, reservation?: Reservation): string {
    const base = 'calendar-day h-12 flex flex-col items-center justify-center text-sm cursor-pointer'

    if (reservation) return `${base} reserved`

    const { checkIn, checkOut } = dateSelection
    if (checkIn && checkOut) {
      if (isSameDay(date, checkIn)) return `${base} selected-start font-semibold`
      if (isSameDay(date, checkOut)) return `${base} selected-end font-semibold`
      if (date > checkIn && date < checkOut) return `${base} selected-range`
    } else if (checkIn && isSameDay(date, checkIn)) {
      return `${base} selected-start font-semibold`
    }

    if (isSameDay(date, today)) return `${base} today font-semibold text-villa-500`

    const dayOfWeek = date.getDay()
    if (dayOfWeek === 0) return `${base} text-red-400`
    if (dayOfWeek === 6) return `${base} text-blue-500`
    return base
  }

  const cells = []

  // Empty cells for days before the 1st
  for (let i = 0; i < firstDay; i++) {
    cells.push(<div key={`empty-${i}`} className="calendar-day empty h-12"></div>)
  }

  // Day cells
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const reservation = getReservationForDate(date)
    const className = getDayClass(date, reservation)

    cells.push(
      <div
        key={day}
        className={className}
        onClick={() => onDayClick(date, reservation)}
      >
        <span>{day}</span>
        {reservation && (
          <span className="text-[10px] opacity-75">{reservation.reserverName}</span>
        )}
        {!reservation && isSameDay(date, today) && (
          <span className="text-[9px] opacity-75">오늘</span>
        )}
        {!reservation && dateSelection.checkIn && isSameDay(date, dateSelection.checkIn) && (
          <span className="text-[10px] opacity-75">입실</span>
        )}
        {!reservation && dateSelection.checkOut && isSameDay(date, dateSelection.checkOut) && (
          <span className="text-[10px] opacity-75">퇴실</span>
        )}
      </div>
    )
  }

  return (
    <div className="glass rounded-3xl shadow-xl shadow-violet-100/50 border border-white/60 overflow-hidden">
      {/* Calendar Header */}
      <div className="p-5 pb-3 flex items-center justify-between">
        <button
          onClick={onPrevMonth}
          className="w-10 h-10 rounded-xl hover:bg-villa-50 flex items-center justify-center transition"
        >
          <i className="ri-arrow-left-s-line text-xl text-gray-600"></i>
        </button>
        <h2 className="text-lg font-bold text-gray-800">{year}년 {month + 1}월</h2>
        <button
          onClick={onNextMonth}
          className="w-10 h-10 rounded-xl hover:bg-villa-50 flex items-center justify-center transition"
        >
          <i className="ri-arrow-right-s-line text-xl text-gray-600"></i>
        </button>
      </div>

      {/* Day Headers */}
      <div className="grid grid-cols-7 px-5 pb-2">
        {DAY_HEADERS.map((name, i) => (
          <div
            key={name}
            className={`text-center text-xs font-semibold py-2 ${
              i === 0 ? 'text-red-400' : i === 6 ? 'text-blue-400' : 'text-gray-400'
            }`}
          >
            {name}
          </div>
        ))}
      </div>

      {/* Calendar Grid */}
      <div className="grid grid-cols-7 gap-1.5 px-5 pb-5">
        {cells}
      </div>

      {/* Legend */}
      <div className="px-5 pb-4 flex items-center gap-4 text-xs text-gray-500">
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded bg-gradient-to-r from-gray-400 to-gray-500"></div>
          <span>예약됨</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded bg-gradient-to-r from-indigo-500 to-purple-600"></div>
          <span>선택한 기간</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded border-2 border-indigo-500"></div>
          <span>오늘</span>
        </div>
      </div>
    </div>
  )
}
