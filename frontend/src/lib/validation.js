const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const USERNAME_REGEX = /^[a-zA-Z0-9_]+$/
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/

export function validateEmail(email) {
  return EMAIL_REGEX.test(email)
}

export function validateUsername(username) {
  return USERNAME_REGEX.test(username)
}

export function validatePassword(password) {
  return PASSWORD_REGEX.test(password)
}

export function mapFieldErrors(details) {
  if (!Array.isArray(details)) {
    return {}
  }

  return details.reduce((acc, detail) => {
    if (detail?.field && detail?.message) {
      acc[detail.field] = detail.message
    }
    return acc
  }, {})
}
