import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Alert from '../components/Alert'
import FormInput from '../components/FormInput'
import LoadingSpinner from '../components/LoadingSpinner'
import { apiRequest, ApiError } from '../lib/api'
import { validatePassword } from '../lib/validation'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') || '', [searchParams])

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccessMessage('')

    if (!token) {
      setError('Missing password reset token.')
      return
    }

    if (!validatePassword(newPassword)) {
      setError('Password must include uppercase, lowercase, number, special character, and be at least 8 characters long.')
      return
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    try {
      setIsSubmitting(true)
      const response = await apiRequest('/api/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword }),
      })

      setSuccessMessage(response?.message || 'Password reset successful. You can now log in.')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Could not reset password. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">Reset password</h1>
      {error ? <Alert type="error">{error}</Alert> : null}
      {successMessage ? <Alert type="success">{successMessage}</Alert> : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <FormInput
          autoComplete="new-password"
          label="New password"
          name="newPassword"
          onChange={(event) => setNewPassword(event.target.value)}
          type="password"
          value={newPassword}
        />
        <FormInput
          autoComplete="new-password"
          label="Confirm new password"
          name="confirmPassword"
          onChange={(event) => setConfirmPassword(event.target.value)}
          type="password"
          value={confirmPassword}
        />

        <button
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? <LoadingSpinner text="Updating..." /> : 'Update password'}
        </button>
      </form>

      <Link className="text-sm text-indigo-600 hover:underline" to="/login">
        Back to login
      </Link>
    </div>
  )
}
