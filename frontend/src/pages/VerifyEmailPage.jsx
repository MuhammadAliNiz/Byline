import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Alert from '../components/Alert'
import LoadingSpinner from '../components/LoadingSpinner'
import { apiRequest, ApiError } from '../lib/api'

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') || '', [searchParams])

  const [status, setStatus] = useState('loading')
  const [message, setMessage] = useState('Verifying your email...')

  useEffect(() => {
    let mounted = true

    const verify = async () => {
      if (!token) {
        setStatus('error')
        setMessage('Verification token is missing.')
        return
      }

      try {
        const response = await apiRequest(`/api/auth/verify-email?token=${encodeURIComponent(token)}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        })

        if (!mounted) return

        setStatus('success')
        setMessage(response?.message || 'Email verified successfully. You can now log in.')
      } catch (err) {
        if (!mounted) return

        setStatus('error')
        if (err instanceof ApiError) {
          setMessage(err.message)
        } else {
          setMessage('Email verification failed. Please try again.')
        }
      }
    }

    verify()

    return () => {
      mounted = false
    }
  }, [token])

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">Verify email</h1>

      {status === 'loading' ? (
        <LoadingSpinner text={message} />
      ) : (
        <Alert type={status === 'success' ? 'success' : 'error'}>{message}</Alert>
      )}

      <Link className="text-sm text-indigo-600 hover:underline" to="/login">
        Continue to login
      </Link>
    </div>
  )
}
