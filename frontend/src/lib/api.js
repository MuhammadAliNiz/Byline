const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

class ApiError extends Error {
  constructor(message, status, details = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

function buildUrl(path) {
  if (!API_BASE_URL) {
    return path
  }

  return `${API_BASE_URL.replace(/\/$/, '')}${path}`
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
  const body = isJson ? await response.json() : null

  if (!response.ok) {
    const message = body?.message || 'Request failed. Please try again.'
    const details = body?.details || []
    throw new ApiError(message, response.status, details)
  }

  return body
}

export async function apiRequest(path, options = {}) {
  const response = await fetch(buildUrl(path), {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })

  return parseResponse(response)
}

export function startGoogleOAuth() {
  window.location.assign(buildUrl('/oauth2/authorization/google'))
}

export { ApiError }
