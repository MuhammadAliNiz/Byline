import { Navigate, useLocation } from 'react-router-dom'
import LoadingSpinner from './LoadingSpinner'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()

  if (isInitializing) {
    return <LoadingSpinner text="Checking session..." />
  }

  if (!isAuthenticated) {
    return <Navigate replace state={{ from: location }} to="/login" />
  }

  return children
}
