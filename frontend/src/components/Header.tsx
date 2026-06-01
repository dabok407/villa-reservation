interface HeaderProps {
  onCheckout: () => void
}

export default function Header({ onCheckout }: HeaderProps) {
  return (
    <header className="hero-gradient text-white shadow-lg">
      <div className="max-w-4xl mx-auto px-4 py-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur">
              <i className="ri-home-heart-line text-xl"></i>
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">아야진 데시앙</h1>
              <p className="text-white/70 text-xs">가족 숙박 예약 시스템</p>
            </div>
          </div>
          <button
            onClick={onCheckout}
            className="bg-white/20 hover:bg-white/30 backdrop-blur px-4 py-2 rounded-xl text-sm font-medium transition flex items-center gap-1.5"
          >
            <i className="ri-logout-box-line"></i> 체크아웃
          </button>
        </div>
      </div>
    </header>
  )
}
