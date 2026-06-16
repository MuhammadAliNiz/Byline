import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Layout({ children }) {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex w-full max-w-5xl items-center justify-between px-4 py-4">
          <Link className="text-lg font-semibold text-indigo-600" to="/">
            Byline Auth UI
          </Link>

          <nav className="flex items-center gap-3 text-sm">
            {isAuthenticated ? (
              <>
                <Link className="hover:text-indigo-600" to="/profile">
                  {user?.username || 'Profile'}
                </Link>
                <button
                  className="rounded-md border border-slate-300 px-3 py-1.5 hover:bg-slate-100"
                  onClick={handleLogout}
                  type="button"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link className="hover:text-indigo-600" to="/login">
                  Login
                </Link>
                <Link className="rounded-md bg-indigo-600 px-3 py-1.5 text-white hover:bg-indigo-700" to="/register">
                  Register
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl px-4 py-10">{children}</main>
    </div>
  )
}
