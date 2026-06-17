import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Alert from '../components/Alert'
import FormInput from '../components/FormInput'
import LoadingSpinner from '../components/LoadingSpinner'
import { useAuth } from '../context/authContext'
import { startGoogleOAuth, ApiError } from '../lib/api'
import { mapFieldErrors, validateEmail } from '../lib/validation'

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const from = location.state?.from?.pathname || '/profile'
  const oauthError = new URLSearchParams(location.search).get('error')

  if (isAuthenticated) {
    return <Navigate replace to="/profile" />
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setFieldErrors({})

    if (!validateEmail(form.email)) {
      setFieldErrors({ email: 'Please enter a valid email address.' })
      return
    }

    if (!form.password.trim()) {
      setFieldErrors({ password: 'Password is required.' })
      return
    }

    try {
      setIsSubmitting(true)
      await login(form.email.trim(), form.password)
      navigate(from, { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(mapFieldErrors(err.details))
      } else {
        setError('Unable to login right now. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">Login</h1>

      {oauthError ? <Alert type="error">OAuth login failed. Please try again.</Alert> : null}
      {error ? <Alert type="error">{error}</Alert> : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <FormInput
          autoComplete="email"
          error={fieldErrors.email}
          label="Email"
          name="email"
          onChange={handleChange}
          placeholder="you@example.com"
          type="email"
          value={form.email}
        />

        <FormInput
          autoComplete="current-password"
          error={fieldErrors.password}
          label="Password"
          name="password"
          onChange={handleChange}
          placeholder="••••••••"
          type="password"
          value={form.password}
        />

        <button
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? <LoadingSpinner text="Signing in..." /> : 'Login'}
        </button>
      </form>

      <button
        className="w-full rounded-md border border-slate-300 px-4 py-2 font-medium hover:bg-slate-100"
        onClick={startGoogleOAuth}
        type="button"
      >
        Continue with Google
      </button>

      <div className="flex items-center justify-between text-sm">
        <Link className="text-indigo-600 hover:underline" to="/forgot-password">
          Forgot password?
        </Link>
        <Link className="text-indigo-600 hover:underline" to="/register">
          Create account
        </Link>
      </div>
    </div>
  )
}
