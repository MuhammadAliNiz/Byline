import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="space-y-3 rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
      <h1 className="text-2xl font-bold">Page not found</h1>
      <p className="text-sm text-slate-600">The page you requested does not exist.</p>
      <Link className="text-indigo-600 hover:underline" to="/">
        Back to home
      </Link>
    </div>
  )
}
