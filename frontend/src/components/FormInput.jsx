export default function FormInput({ label, name, type = 'text', value, onChange, placeholder, autoComplete, error }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <input
        autoComplete={autoComplete}
        className={`w-full rounded-md border px-3 py-2 outline-none transition ${
          error ? 'border-red-400 focus:ring-2 focus:ring-red-100' : 'border-slate-300 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100'
        }`}
        name={name}
        onChange={onChange}
        placeholder={placeholder}
        type={type}
        value={value}
      />
      {error ? <span className="text-xs text-red-600">{error}</span> : null}
    </label>
  )
}
