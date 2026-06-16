export default function LoadingSpinner({ text = 'Loading...' }) {
  return (
    <div className="flex items-center gap-2 text-sm text-slate-600">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
      <span>{text}</span>
    </div>
  )
}
