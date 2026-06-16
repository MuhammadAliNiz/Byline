import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Alert from '../components/Alert'
import LoadingSpinner from '../components/LoadingSpinner'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../lib/api'

export default function OAuthCallbackPage() {
  const { refreshSession } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true

    const completeOAuth = async () => {
      try {
        await refreshSession()
        navigate('/profile', { replace: true })
      } catch (err) {
        if (!mounted) return
        if (err instanceof ApiError) {
          setError(err.message)
        } else {
          setError('OAuth login could not be completed. Please try again.')
        }
      }
    }

    completeOAuth()

    return () => {
      mounted = false
    }
  }, [refreshSession, navigate])

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">OAuth login</h1>
      {!error ? <LoadingSpinner text="Finishing OAuth login..." /> : <Alert type="error">{error}</Alert>}
      <Link className="text-sm text-indigo-600 hover:underline" to="/login">
        Back to login
      </Link>
    </div>
  )
}
