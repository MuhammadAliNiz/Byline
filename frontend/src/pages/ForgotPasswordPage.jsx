import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../components/Alert'
import FormInput from '../components/FormInput'
import LoadingSpinner from '../components/LoadingSpinner'
import { apiRequest, ApiError } from '../lib/api'
import { validateEmail } from '../lib/validation'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccessMessage('')

    if (!validateEmail(email.trim())) {
      setError('Please enter a valid email address.')
      return
    }

    try {
      setIsSubmitting(true)
      const response = await apiRequest('/api/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim() }),
      })

      setSuccessMessage(
        response?.message || 'If an account with that email exists, a password reset link has been sent.',
      )
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Could not send reset email. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">Forgot password</h1>
      <p className="text-sm text-slate-600">Enter your email and we will send you a password reset link.</p>

      {error ? <Alert type="error">{error}</Alert> : null}
      {successMessage ? <Alert type="success">{successMessage}</Alert> : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <FormInput
          autoComplete="email"
          label="Email"
          name="email"
          onChange={(event) => setEmail(event.target.value)}
          type="email"
          value={email}
        />

        <button
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? <LoadingSpinner text="Sending..." /> : 'Send reset link'}
        </button>
      </form>

      <Link className="text-sm text-indigo-600 hover:underline" to="/login">
        Back to login
      </Link>
    </div>
  )
}
