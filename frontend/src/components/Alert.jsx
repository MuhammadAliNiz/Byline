export default function Alert({ type = 'info', children }) {
  const styles = {
    error: 'border-red-300 bg-red-50 text-red-700',
    success: 'border-emerald-300 bg-emerald-50 text-emerald-700',
    info: 'border-blue-300 bg-blue-50 text-blue-700',
  }

  return <div className={`rounded-md border px-4 py-3 text-sm ${styles[type] || styles.info}`}>{children}</div>
}
