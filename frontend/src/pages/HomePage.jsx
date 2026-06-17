import { Link } from 'react-router-dom'
import { useAuth } from '../context/authContext'

export default function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="space-y-5 rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
      <h1 className="text-3xl font-bold">Welcome to Byline Frontend</h1>
      <p className="text-slate-600">
        This is a basic React + Tailwind authentication UI for your Spring Boot backend with email/password and Google OAuth login.
      </p>
      <div className="flex flex-wrap gap-3">
        {isAuthenticated ? (
          <Link className="rounded-md bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700" to="/profile">
            Open Profile
          </Link>
        ) : (
          <>
            <Link className="rounded-md bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700" to="/login">
              Login
            </Link>
            <Link className="rounded-md border border-slate-300 px-4 py-2 hover:bg-slate-100" to="/register">
              Register
            </Link>
          </>
        )}
      </div>
    </div>
  )
}
