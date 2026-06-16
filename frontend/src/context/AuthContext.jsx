import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { ApiError, apiRequest } from '../lib/api'

const ACCESS_TOKEN_KEY = 'byline_access_token'
const USER_KEY = 'byline_user'

const AuthContext = createContext(null)

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem(ACCESS_TOKEN_KEY))
  const [user, setUser] = useState(() => readStoredUser())
  const [isInitializing, setIsInitializing] = useState(true)

  const isAuthenticated = Boolean(accessToken)

  const persistAuth = (token, authUser) => {
    setAccessToken(token)
    setUser(authUser)

    localStorage.setItem(ACCESS_TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(authUser))
  }

  const clearAuth = () => {
    setAccessToken(null)
    setUser(null)
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  const refreshSession = async () => {
    const response = await apiRequest('/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    const loginData = response?.data
    if (!loginData?.accessToken) {
      throw new ApiError('Invalid refresh response from server.', 500)
    }

    persistAuth(loginData.accessToken, loginData.user)
    return loginData
  }

  useEffect(() => {
    let active = true

    const bootstrap = async () => {
      try {
        await refreshSession()
      } catch {
        clearAuth()
      } finally {
        if (active) {
          setIsInitializing(false)
        }
      }
    }

    bootstrap()

    return () => {
      active = false
    }
  }, [])

  const login = async (email, password) => {
    const response = await apiRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })

    const loginData = response?.data
    persistAuth(loginData.accessToken, loginData.user)
    return loginData
  }

  const register = async (payload) => {
    return apiRequest('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  }

  const logout = async () => {
    try {
      await apiRequest('/api/auth/logout', { method: 'POST', body: JSON.stringify({}) })
    } finally {
      clearAuth()
    }
  }

  const getMe = async () => {
    if (!isAuthenticated) {
      throw new ApiError('Not authenticated', 401)
    }

    try {
      const response = await apiRequest('/api/users/me', {
        method: 'GET',
        headers: {
          Authorization: 'Bearer ' + accessToken,
        },
      })

      return response?.data
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        const refreshed = await refreshSession()
        const retried = await apiRequest('/api/users/me', {
          method: 'GET',
          headers: {
            Authorization: 'Bearer ' + refreshed.accessToken,
          },
        })
        return retried?.data
      }

      throw error
    }
  }

  const value = useMemo(
    () => ({
      accessToken,
      user,
      isAuthenticated,
      isInitializing,
      login,
      register,
      logout,
      refreshSession,
      getMe,
    }),
    [accessToken, user, isAuthenticated, isInitializing],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
