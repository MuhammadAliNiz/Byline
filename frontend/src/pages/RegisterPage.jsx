import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../components/Alert'
import FormInput from '../components/FormInput'
import LoadingSpinner from '../components/LoadingSpinner'
import { useAuth } from '../context/authContext'
import { ApiError } from '../lib/api'
import { mapFieldErrors, validateEmail, validatePassword, validateUsername } from '../lib/validation'

const INITIAL_FORM = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
}

export default function RegisterPage() {
  const { register } = useAuth()
  const [form, setForm] = useState(INITIAL_FORM)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const validate = () => {
    const nextErrors = {}

    if (!form.firstName.trim()) nextErrors.firstName = 'First name is required.'
    if (!form.lastName.trim()) nextErrors.lastName = 'Last name is required.'

    if (!validateUsername(form.username.trim())) {
      nextErrors.username = 'Username can only contain letters, numbers, and underscores.'
    }

    if (!validateEmail(form.email.trim())) {
      nextErrors.email = 'Please enter a valid email address.'
    }

    if (!validatePassword(form.password)) {
      nextErrors.password = 'Use 8+ chars with uppercase, lowercase, number, and special character.'
    }

    return nextErrors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccessMessage('')

    const nextErrors = validate()
    setFieldErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    try {
      setIsSubmitting(true)
      const response = await register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
      })

      setSuccessMessage(response?.message || 'Registration successful. Please check your email.')
      setForm(INITIAL_FORM)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors((prev) => ({ ...prev, ...mapFieldErrors(err.details) }))
      } else {
        setError('Unable to register right now. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">Create account</h1>

      {error ? <Alert type="error">{error}</Alert> : null}
      {successMessage ? <Alert type="success">{successMessage}</Alert> : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <FormInput error={fieldErrors.firstName} label="First name" name="firstName" onChange={handleChange} value={form.firstName} />
        <FormInput error={fieldErrors.lastName} label="Last name" name="lastName" onChange={handleChange} value={form.lastName} />
        <FormInput error={fieldErrors.username} label="Username" name="username" onChange={handleChange} value={form.username} />
        <FormInput error={fieldErrors.email} label="Email" name="email" onChange={handleChange} type="email" value={form.email} />
        <FormInput
          autoComplete="new-password"
          error={fieldErrors.password}
          label="Password"
          name="password"
          onChange={handleChange}
          type="password"
          value={form.password}
        />

        <button
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? <LoadingSpinner text="Creating account..." /> : 'Register'}
        </button>
      </form>

      <p className="text-sm text-slate-600">
        Already have an account?{' '}
        <Link className="text-indigo-600 hover:underline" to="/login">
          Login
        </Link>
      </p>
    </div>
  )
}
